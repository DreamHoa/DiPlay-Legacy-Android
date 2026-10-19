import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('public_tree', ROOT / 'scripts/check_public_tree.py')
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


class PublicTreeTest(unittest.TestCase):
    def test_pinned_public_ca_bundle_is_allowed_with_either_line_ending(self):
        name = 'common/src/main/res/raw/update_ca_bundle.pem'
        body = (ROOT / name).read_bytes().replace(b'\r\n', b'\n')
        self.assertFalse(checker.forbidden(name, body))
        self.assertFalse(checker.forbidden(name, body.replace(b'\n', b'\r\n')))

    def test_changed_bundle_and_other_pem_files_are_rejected(self):
        name = 'common/src/main/res/raw/update_ca_bundle.pem'
        body = (ROOT / name).read_bytes()
        self.assertTrue(checker.forbidden(name, body + b'changed'))
        self.assertTrue(checker.forbidden('other.pem', body))

    def test_identity_and_distribution_containers_remain_rejected(self):
        for name in ['identity.pk8', 'certificate.p7b', 'signing.jks', 'app.apk', '.private/config.txt']:
            self.assertTrue(checker.forbidden(name, b'not empty'), name)

    def test_private_key_blocks_are_rejected_even_in_text_files(self):
        key = b'-----BEGIN PRIVATE KEY-----\n' + b'A' * 64 + b'\n-----END PRIVATE KEY-----'
        self.assertTrue(checker.forbidden('notes.txt', key))


if __name__ == '__main__':
    unittest.main()
