from openai import OpenAI
import os
import logging

MODEL = "ep-20250214202830-btfll"

TEMPERATURE = 1.0

def set_temperature(temp):
    global TEMPERATURE
    if isinstance(temp, float):
        TEMPERATURE = temp
    else:
        logging.warning(f"Invalid temperature value: {temp}. Using default value {TEMPERATURE}.")
        TEMPERATURE = 1.0

def doubao_jsonl(prompt, custom_id):
    return {
        "custom_id": custom_id, 
        "body": {
            "messages": 
                [
                    {"role": "system", "content": "You are an expert who are familar with static analysis tools --Semgrep."},
                    {"role": "user", "content": prompt}
                ],
            "max_tokens": 8192,
            "temperature": TEMPERATURE,
        }
    }

def chat(prompt):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("DOUBAO_APIKEY"), base_url="https://ark.cn-beijing.volces.com/api/v3")
    response = client.chat.completions.create(
        #model="ep-20250214160226-7mr86",
        model = MODEL,
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
    client = OpenAI(api_key=os.getenv("DOUBAO_APIKEY"), base_url="https://ark.cn-beijing.volces.com/api/v3")
    response = client.chat.completions.create(
        #model="ep-20250214160226-7mr86",
        model = MODEL,
        messages=msg,
        max_tokens=8192,
        temperature=TEMPERATURE,
        stream=False
    )
    return response.choices[0].message.content


def chat_raw(prompt):
    global TEMPERATURE
    client = OpenAI(api_key=os.getenv("DOUBAO_APIKEY"), base_url="https://ark.cn-beijing.volces.com/api/v3")
    response = client.chat.completions.create(
        #model="ep-20250214160226-7mr86",
        model = MODEL,
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
        for i in range(2):
            futures.append(executor.submit(chat_raw, "Hello"))
        for future in concurrent.futures.as_completed(futures):
            print(future.result())