from typing import Any
from openai import OpenAI, AsyncOpenAI

class BaseModel:
    id: Any
    __name__: str


class LLM:
    def __init__(self, model_version: str, api_key: str) -> None:
        self.model_version = model_version
        
        self.client = OpenAI(api_key=api_key)
        self.aclient = AsyncOpenAI(api_key=api_key)
        
    def gpt(self, messages: list):
        response = self.client.chat.completions.create(
            model=self.model_version,
            messages=messages
        )
        
        return response.choices[0].message.content

    async def astream(self, messages: list):
        stream = await self.aclient.chat.completions.create(
            model=self.model_version,
            messages=messages,
            stream=True
        )
        return stream
    
        

        