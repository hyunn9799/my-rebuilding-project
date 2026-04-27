from typing import Any
import requests
import json

class BaseModel:
    id: Any
    __name__: str


class STT:
    def __init__(self, model_name: str, secret_key: str, url: str) -> None:
        if model_name == "naver":
            self.secret_key = secret_key
            self.url = url
        
    def clova(self, audio_resp: requests.Response):
        headers = {"X-CLOVASPEECH-API-KEY": self.secret_key}
        params = {
            "language": "ko-KR",
            "completion": "sync",
            "fullText": True,
            "diarization": {"enable": False}
        }
        files = {
            "media": ("speech.wav", audio_resp.content, "audio/wav"),
            "params": (None, json.dumps(params), "application/json"),
        }

        resp = requests.post(self.url, headers=headers, files=files, timeout=120)
        
        return resp
        

        