from openai import OpenAI
import os
import logging

TEMPERATURE = 1.0

def set_temperature(temp):
    global TEMPERATURE
    if isinstance(temp, float):
        TEMPERATURE = temp
        logging.info(f"Temperature set to {TEMPERATURE}.")
    else:
        logging.warning(f"Invalid temperature value: {temp}. Using default value {TEMPERATURE}.")
        TEMPERATURE = 1.0

def chat(prompt):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("UIUI_API_KEY"), base_url="https://sg.uiuiapi.com/v1")
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "You are an expert who are familar with static analysis tools --Semgrep."},
            {"role": "user", "content": prompt},
        ],
        max_tokens=8192,
        temperature=TEMPERATURE,
        stream=False
    )
    return response.choices[0].message.content

def chat2(msg):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("UIUI_API_KEY"), base_url="https://sg.uiuiapi.com/v1")
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=msg,
        max_tokens=8192,
        temperature=TEMPERATURE,
        stream=False
    )
    return response.model_dump_json()

def chat_raw(prompt):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("UIUI_API_KEY"), base_url="https://sg.uiuiapi.com/v1")
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "You are an expert who are familar with static analysis tools --Semgrep."},
            {"role": "user", "content": prompt},
        ],
        max_tokens=8192,
        temperature=TEMPERATURE,
        stream=False
    )
    return response.model_dump_json()

if __name__ == "__main__":
    import concurrent.futures
    with concurrent.futures.ThreadPoolExecutor() as executor:
        futures = []
        for i in range(10):
            futures.append(executor.submit(chat, "Hello"))
        for future in concurrent.futures.as_completed(futures):
            print(future.result())