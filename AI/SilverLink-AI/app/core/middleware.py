from functools import wraps
import inspect

from dependency_injector.wiring import inject as di_inject
from loguru import logger

from app.callbot.services.base_service import BaseService as CallbotService
from app.chatbot.services.base_service import BaseService as ChatbotService
from app.ocr.services.base_service import BaseService as OcrService

def inject_callbot(func):
    if inspect.iscoroutinefunction(func):
        @di_inject
        @wraps(func)
        async def wrapper(*args, **kwargs):
            try:
                # print(f"Middleware: calling {func.__name__}")
                result = await func(*args, **kwargs)
                injected_services = [arg for arg in kwargs.values() if isinstance(arg, CallbotService)]
                if len(injected_services) > 0:
                    try:
                        injected_services[-1].close_scoped_session()
                    except Exception as e:
                        logger.error(e)
                return result
            except Exception as e:
                logger.error(f"Middleware Error in {func.__name__}: {e}")
                raise e
    else:
        @di_inject
        @wraps(func)
        def wrapper(*args, **kwargs):
            result = func(*args, **kwargs)
            injected_services = [arg for arg in kwargs.values() if isinstance(arg, CallbotService)]
            if len(injected_services) > 0:
                try:
                    injected_services[-1].close_scoped_session()
                except Exception as e:
                    logger.error(e)
            return result

    return wrapper

def inject_chatbot(func):
    if inspect.iscoroutinefunction(func):
        @di_inject
        @wraps(func)
        async def wrapper(*args, **kwargs):
            result = await func(*args, **kwargs)
            injected_services = [arg for arg in kwargs.values() if isinstance(arg, ChatbotService)]
            if len(injected_services) > 0:
                try:
                    injected_services[-1].close_scoped_session()
                except Exception as e:
                    logger.error(e)
            return result
    else:
        @di_inject
        @wraps(func)
        def wrapper(*args, **kwargs):
            result = func(*args, **kwargs)
            injected_services = [arg for arg in kwargs.values() if isinstance(arg, ChatbotService)]
            if len(injected_services) > 0:
                try:
                    injected_services[-1].close_scoped_session()
                except Exception as e:
                    logger.error(e)
            return result

    return wrapper

def inject_ocr(func):
    if inspect.iscoroutinefunction(func):
        @di_inject
        @wraps(func)
        async def wrapper(*args, **kwargs):
            result = await func(*args, **kwargs)
            injected_services = [arg for arg in kwargs.values() if isinstance(arg, OcrService)]
            if len(injected_services) > 0:
                try:
                    injected_services[-1].close_scoped_session()
                except Exception as e:
                    logger.error(e)
            return result
    else:
        @di_inject
        @wraps(func)
        def wrapper(*args, **kwargs):
            result = func(*args, **kwargs)
            injected_services = [arg for arg in kwargs.values() if isinstance(arg, OcrService)]
            if len(injected_services) > 0:
                try:
                    injected_services[-1].close_scoped_session()
                except Exception as e:
                    logger.error(e)
            return result

    return wrapper
