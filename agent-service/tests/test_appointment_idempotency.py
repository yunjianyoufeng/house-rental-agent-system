import unittest
from datetime import datetime, timedelta
from unittest.mock import AsyncMock, patch

from app.tools.appointment_tools import confirm_appointment, prepare_appointment, is_explicit_confirmation


class AppointmentIdempotencyTest(unittest.IsolatedAsyncioTestCase):
    async def test_prepare_generates_stable_request_key(self):
        appointment_time = (datetime.now().astimezone() + timedelta(days=1)).isoformat(
            timespec="seconds"
        )
        arguments = {
            "house_id": 19,
            "appointment_time": appointment_time,
            "remark": "上午看房",
        }
        with (
            patch(
                "app.tools.appointment_tools.get_house_detail",
                new=AsyncMock(
                    return_value={
                        "id": 19,
                        "title": "近医院电梯两室",
                        "address": "历下区测试地址",
                        "rentPrice": 2300,
                    }
                ),
            ),
            patch(
                "app.tools.appointment_tools.set_json_state",
                new=AsyncMock(),
            ),
        ):
            first = await prepare_appointment(arguments, "conversation-1", 8)
            second = await prepare_appointment(arguments, "conversation-1", 8)

        self.assertEqual(first["requestKey"], second["requestKey"])
        self.assertEqual(64, len(first["requestKey"]))
        self.assertNotEqual(first["confirmationVersion"], second["confirmationVersion"])

    async def test_confirm_forwards_request_key(self):
        pending = {
            "houseId": 19,
            "houseTitle": "近医院电梯两室",
            "appointmentTime": "2026-08-24T10:00:00+08:00",
            "remark": "上午看房",
            "requestKey": "stable-request-key",
            "confirmationVersion": "version-at-start",
        }
        with (
            patch(
                "app.tools.appointment_tools.get_pending_appointment",
                new=AsyncMock(return_value=pending),
            ),
            patch(
                "app.tools.appointment_tools.post_backend_data",
                new=AsyncMock(return_value="预约成功"),
            ) as backend,
            patch(
                "app.tools.appointment_tools.delete_state",
                new=AsyncMock(return_value=True),
            ),
        ):
            await confirm_appointment(
                "conversation-1",
                8,
                "Bearer token",
                True,
                "version-at-start",
                True,
            )

        self.assertEqual(
            "stable-request-key",
            backend.await_args.args[1]["requestKey"],
        )

    async def test_changed_pending_requires_another_confirmation(self):
        with (
            patch("app.tools.appointment_tools.get_pending_appointment",
                  new=AsyncMock(return_value={"confirmationVersion": "new-version"})),
            patch("app.tools.appointment_tools.post_backend_data", new=AsyncMock()) as backend,
        ):
            with self.assertRaisesRegex(ValueError, "已变更"):
                await confirm_appointment("conversation-1", 8, "Bearer token", True, "old-version", True)
        backend.assert_not_awaited()

    async def test_model_cannot_confirm_without_user_confirmation(self):
        with (
            patch("app.tools.appointment_tools.get_pending_appointment",
                  new=AsyncMock(return_value={"confirmationVersion": "version"})),
            patch("app.tools.appointment_tools.post_backend_data", new=AsyncMock()) as backend,
        ):
            with self.assertRaisesRegex(ValueError, "单独回复"):
                await confirm_appointment("conversation-1", 8, "Bearer token", True, "version", False)
        backend.assert_not_awaited()

    def test_confirmation_does_not_accept_negation_or_edits(self):
        for message in ("不要确认预约", "把时间改到明天然后确认预约", "他说确认预约", "可以取消吗？"):
            with self.subTest(message=message):
                self.assertFalse(is_explicit_confirmation(message))
        for message in ("确认预约", "好的", "可以！"):
            self.assertTrue(is_explicit_confirmation(message))


if __name__ == "__main__":
    unittest.main()
