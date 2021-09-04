import json

import requests
import uvicorn
from fastapi import FastAPI, Query, Path, HTTPException
from sec import mipush_sec

app = FastAPI()

api_url = "https://api.xmpush.xiaomi.com/v2/message/user_account"

with open('user.json') as f:
    users = json.dumps(f.read())

notify_id = 120345

api_caller = requests.session()
api_caller.headers['Authorization'] = f'key={mipush_sec}'


@app.get('/{user_id}/check')
async def check(*, user_id: str = Path(..., description="User ID")):
    if users.find(user_id) != -1:
        return True
    return False


@app.get('/{user_id}/send')
async def send(*,
               user_id: str = Path(..., description="User ID"),
               title: str = Query(default="Title", description="Notification Title"),
               content: str = Query(..., description="Notification Content"),
               long: str = Query(default="", description="Long Message")
               ):
    if users.find(user_id) == -1:
        raise HTTPException(403, "Unauthorized")

    global notify_id
    notify_id += 1

    post_data = {
        'payload': long,
        'restricted_package_name': 'top.learningman.mipush',
        'pass_through': 0,
        'title': title,
        'description': content,
        'notify_id': notify_id,
        'user_account': user_id
    }

    resp = api_caller.post(api_url, data=post_data)

    return resp.json()


if __name__ == '__main__':
    uvicorn.run("main:app", host="0.0.0.0", port=14444, debug=True)
