from fastapi import APIRouter

from app.api.endpoints.callbot import router as callbot_router
from app.api.endpoints.chatbot import router as chatbot_router
from app.api.endpoints.ocr import router as ocr_router

routers = APIRouter()
router_list = [callbot_router,chatbot_router,ocr_router]

for router in router_list:
    routers.include_router(router)
