import os
from openai import OpenAI
import logging

TEMPERATURE = 1.0

def set_temperature(temp):
    global TEMPERATURE
    if isinstance(temp, float):
        TEMPERATURE = temp
        logging.info(f"Temperature set to {TEMPERATURE}.")
    else:
        print(f"Invalid temperature value: {temp}. Using default value {TEMPERATURE}.")
        TEMPERATURE = 1.0

def chat(prompt):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("DASHSCOPE_API_KEY"), base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")
    response = client.chat.completions.create(
        #model candidate list: qwen-max , qwen-turbo ,qwen-plus
        model="qwen-plus",  
        messages=[
            {"role": "user", "content": prompt},
        ],
        max_tokens=8192,
        temperature=TEMPERATURE,
        stream=False,
        extra_body={"enable_thinking": False},
    )
    return response.choices[0].message.content

def chat2(prompt):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("DASHSCOPE_API_KEY"), base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")
    response = client.chat.completions.create(
        #model candidate list: qwen-max , qwen-turbo ,qwen-plus
        model="qwen-plus",  
        messages=[
            {"role": "system", "content": "You are an expert who are familar with static analysis tools --Semgrep."},
            {"role": "user", "content": prompt},
        ],
        max_tokens=8192,
        temperature=TEMPERATURE,
        stream=False,
        extra_body={"enable_thinking": False},
    )
    return response.choices[0].message.content

def chat_raw(prompt):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("DASHSCOPE_API_KEY"), base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")
    response = client.chat.completions.create(
        #model candidate list: qwen-max , qwen-turbo ,qwen-plus
        model="qwen-plus",  
        messages=[
            {"role": "system", "content": "You are an expert who are familar with static analysis tools --Semgrep."},
            {"role": "user", "content": prompt},
        ],
        max_tokens=8192,
        temperature=TEMPERATURE,
        stream=False,
        extra_body={"enable_thinking": False},
    )
    return response.model_dump_json()

if __name__ == "__main__":
    prompt="what is semgrep?"
    response = chat2(prompt)
    print(response)