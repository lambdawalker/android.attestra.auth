"""Offline release policy and real Git reservation/finalization checks."""
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release_identity as identity
import module_release as m
import release
import jitpack_build
import jitpack_release
import import_docs

class SourceTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)
        self.git('init','-q','--initial-branch=main')
        self.git('config','user.name','Test');self.git('config','user.email','test@example.invalid')
        self.write('auth/src/main/source.kt','one');self.write('gradle.properties','GROUP=io.github.lambdawalker\nPOM_ARTIFACT_ID=attestra-auth\n')
        self.commit();self.original=self.git('rev-parse','HEAD')
    def git(self,*args):return subprocess.check_output(['git',*args],cwd=self.root,text=True,stderr=subprocess.PIPE).strip()
    def write(self,path,value):
        p=self.root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(value)
    def commit(self):self.git('add','.');self.git('commit','-qm','source')
    def choose(self,requested=''):
        return identity.select('auth',self.git('rev-parse','HEAD'),self.git,self.git('tag').splitlines(),[],requested)
    def test_bootstrap_and_patch_semantic_order(self):
        self.assertEqual(('0.1.0',self.original),self.choose())
        self.git('tag','auth/v0.1.9');self.write('auth/src/main/source.kt','two');self.commit()
        self.assertEqual('0.1.10',self.choose()[0])
    def test_docs_only_change_reuses_original_but_buildsrc_change_bumps(self):
        self.git('tag','auth/v0.1.0');self.write('IMPORT.md','confirmed docs');self.commit()
        self.assertEqual(('0.1.0',self.original),self.choose())
        self.write('buildSrc/src/main/kotlin/PrepareCardModelAssets.kt','new');self.commit()
        self.assertEqual('0.1.1',self.choose()[0])
    def test_explicit_reuse_and_malformed_versions_rejected(self):
        self.git('tag','auth/v0.1.0')
        for version in ('0.1.0','0.0.9','01.0.0','1.0','1.0.0-rc1','$(bad)'):
            with self.subTest(version=version),self.assertRaises(ValueError): self.choose(version)
    def test_conflicting_source_tags_rejected(self):
        self.git('tag','auth/v0.1.0');self.write('auth/src/main/source.kt','two');self.commit();self.git('tag','jitpack/auth/v0.1.0')
        with self.assertRaisesRegex(ValueError,'Conflicting'):self.choose()
    def test_real_reservation_prevents_second_upload_and_finalizes_atomically(self):
        remote=self.root/'remote.git'
        subprocess.run(['git','init','--bare',str(remote)],check=True,capture_output=True)
        self.git('remote','add','origin',str(remote));self.git('push','-u','origin','main')
        self.write('docs/templates/IMPORT.md.template','# Install\n{{INSTALLATION}}\n')
        self.commit();self.git('push','origin','main');source=self.git('rev-parse','HEAD')
        record=dict(module='auth',version='0.1.0',source=source,group='io.github.lambdawalker',artifact='attestra-auth',sha256={s:'b'*64 for s in release.SUFFIXES},model_assets={'assets/test.tflite':'c'*64},phase='reserved-before-upload',repository='maven-central',repository_url=release.CENTRAL)
        with patch.object(m,'ROOT',self.root),patch.object(m,'local_record',return_value=record),patch.object(m,'fetch',return_value=None):
            m.reserve('auth','0.1.0',source)
            self.assertEqual(source,self.git('rev-parse','auth/v0.1.0^{commit}'))
            m.guard('auth','0.1.0')
            with self.assertRaisesRegex(ValueError,'already started'):m.guard('auth','0.1.0')
            with self.assertRaisesRegex(ValueError,'Unresolved'):m.ensure_available('auth',m.remote_tags())
            self.write('build/confirmed-auth.json',json.dumps(dict(record,phase='confirmed-public')))
            m.finalize('auth','0.1.0',source)
            self.assertNotIn('release-pending/auth/0.1.0',m.remote_tags())
            self.assertNotIn('release-uploading/auth/0.1.0',m.remote_tags())
            self.assertIn('Confirmed version: **0.1.0**',(self.root/'IMPORT.md').read_text())
            self.assertEqual('true',m.prepare_finalization('auth','0.1.0')['skip'])
            self.assertTrue((self.root/'docs/releases/history/auth/0.1.0.json').is_file())
            self.git('tag','auth/v0.0.9',source);self.git('push','origin','auth/v0.0.9')
            with self.assertRaisesRegex(ValueError,'archiv'):m.prepare_finalization('auth','0.0.9')

class PublicationTests(unittest.TestCase):
    def test_only_auth_tag_and_task_supported(self):
        self.assertEqual(('auth','1.2.3'),jitpack_build.selection('auth~v1.2.3'))
        for value in ('main','v1.2.3','app/v1.2.3','auth/v01.2.3','identity-mock/v1.0.0'):
            with self.assertRaises(ValueError):jitpack_build.selection(value)
        command=jitpack_build.command('auth','1.2.3')
        self.assertIn(':auth:publishToMavenLocal',command)
        self.assertFalse(any('MavenCentral' in c for c in command))
    def test_jitpack_requires_exact_source_and_success(self):
        record=dict(module='auth',version='1.2.3',consumer_version='auth~v1.2.3',group=jitpack_release.GROUP,artifact=jitpack_release.ARTIFACT,source='a'*40)
        for status,source,error in [('building','a'*40,jitpack_release.NotReady),('ok','b'*40,ValueError),('error','a'*40,ValueError)]:
            with self.subTest(status=status),self.assertRaises(error):
                jitpack_release.verify(record,lambda *a,**k:json.dumps({'status':status,'commit':source}).encode())
    def test_pending_records_do_not_advertise_installation(self):
        result=import_docs.render({('maven-central','auth'):dict(phase='reserved-before-upload')},{'auth':('io.github.lambdawalker','attestra-auth')})
        self.assertIn('No confirmed release',result)
    def test_documentation_archive_rejects_binary_media(self):
        data=io.BytesIO()
        with zipfile.ZipFile(data,'w') as z:z.writestr('video.mp4',b'bad')
        with self.assertRaises(ValueError):m.verify_documentation_contents(data.getvalue())
    def test_archive_rejects_empty_aar_and_metadata(self):
        data=io.BytesIO()
        with zipfile.ZipFile(data,'w') as z:z.writestr('AndroidManifest.xml','manifest')
        with self.assertRaises(ValueError):release.verify_artifact(data.getvalue(),'.aar','g','a','1.0.0')
        with self.assertRaises(ValueError):release.verify_artifact(b'{}','.module','g','a','1.0.0')

if __name__=='__main__':unittest.main()
