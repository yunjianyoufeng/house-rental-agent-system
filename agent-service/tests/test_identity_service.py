import unittest
from unittest.mock import AsyncMock, patch

from app.services.identity_service import resolve_authenticated_identity


class IdentityServiceTest(unittest.IsolatedAsyncioTestCase):
    async def test_uses_backend_verified_identity(self):
        with patch(
            "app.services.identity_service.get_backend_data",
            new=AsyncMock(
                return_value={"id": 8, "roleCode": "TENANT", "status": 1}
            ),
        ) as backend:
            identity = await resolve_authenticated_identity("Bearer trusted-token")

        self.assertEqual((8, "TENANT"), identity)
        backend.assert_awaited_once_with(
            "/auth/me",
            headers={"Authorization": "Bearer trusted-token"},
        )

    async def test_rejects_missing_authorization(self):
        with self.assertRaisesRegex(ValueError, "重新登录"):
            await resolve_authenticated_identity(None)

    async def test_rejects_disabled_identity(self):
        with patch(
            "app.services.identity_service.get_backend_data",
            new=AsyncMock(
                return_value={"id": 8, "roleCode": "TENANT", "status": 0}
            ),
        ):
            with self.assertRaisesRegex(ValueError, "禁用"):
                await resolve_authenticated_identity("Bearer disabled-token")


if __name__ == "__main__":
    unittest.main()
