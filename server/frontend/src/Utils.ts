export function isDebug() {
    return process.env.NODE_ENV === 'development';
}

const BASE_URL = isDebug() ? 'http://localhost:14444' : ''

export async function checkUser(userId: string): Promise<boolean> {
    const result = await fetch(`${BASE_URL}/${encodeURIComponent(userId)}/check`).then(res => res.text())
    return result === 'true'
}

export async function sendNotify(userId: string, title: string, content: string, long: string): Promise<void> {
    const data = new URLSearchParams()
    data.append('title', title)
    data.append('content', content)
    data.append('long', long)

    const ret = await fetch(`${BASE_URL}/${encodeURIComponent(userId)}/send`, {
        method: 'POST',
        body: data
    })

    if (ret.ok) {
        return
    } else {
        throw Error(ret.statusText)
    }
}
