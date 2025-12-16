steps Case 
url：https://www.qa-my.carsome.com/website/login/login
post
headers：
{
  "sec-fetch-mode": "cors",
  "Cookie": "__cf_bm=UF0v3_KhxZJZVDhcSeY7X34SfgR38BV8LcVZkiV4ZOM-1764061331.3478658-1.0.1.1-0nHFngqUR4yAH.aXVwhyVD0RhL9UIs0Ykm2ZFCKHoRxQzOi25uPU2iihh4wpmqLukHP.SP_VsOUUKkeL774x1dznQyvt2ztQtbBSbfaIrJzX5BG66Dbx_5YZT7LLUfBv; __cf_bm=BOk8OJuI.TJb8c9NRN3DIhlglYlQjt5dTp_8QjQq2ps-1765250635.8717747-1.0.1.1-7jwUPfM4z40NB8QOjq_2mN.7eHetIna9ICUBZL2BJpxlzxJyiAIfqDlxHqfvtsk1KfLH87R7fSHr0U3EV_3N5bkozbnyC4qFGFQ00gnsOd8aMoYZ7uVaPOy5gx11Eyn4",
  "country": "MY",
  "referer": "https://www.qa-my.carsome.com/",
  "sec-fetch-site": "same-origin",
  "accept-language": "en-GB,en-US;q=0.9,en;q=0.8",
  "origin": "https://www.qa-my.carsome.com",
  "x-language": "en",
  "priority": "u=1, i",
  "accept": "application/json, text/plain, */*",
  "sec-ch-ua": "",
  "sec-ch-ua-mobile": "?1",
  "sec-ch-ua-platform": "",
  "x-platform": "web",
  "content-type": "application/json",
  "sec-fetch-dest": "empty",
  "user-agent": "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"
}
Body:
{"userType":1,"username":"0127712792","captcha":"666666","code":"","source":"HeaderLogin"}
Extractor #1
Source: JSON Body
Expression: $.token
Variable Name: token (前端输入 $token，后端会自动去除 $ 前缀存储为 token)

注意：根据实际响应结构，token 在根级别（不在 data 对象内），所以 JSONPath 表达式应为 $.token
response:
{
    "code": "0000",
    "data": {
        "customer": {
            "id": 24417,
            "customerId": "24040100004",
            "name": "shashi",
            "email": "shashi.periasamy@icarasia.com",
            "contactNo": "0127712792",
            "userType": 2,
            "skipCollect": true,
            "signUp": false,
            "unifiedId": "973bc25b-999d-794e-bc37-2c515194841c",
            "sourceId": 0,
            "category": 0,
            "interested": "",
            "otp": 0,
            "displayPDPA": false
        },
        "LeadUpdateRes": {
            "ID": 0,
            "leadPhoneNo": "",
            "leadFormLevel": ""
        },
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJDdXN0b21lcklkIjoiMjQwNDAxMDAwMDQiLCJJRCI6MjQ0MTcsIlVuaWZpZWRJZCI6Ijk3M2JjMjViLTk5OWQtNzk0ZS1iYzM3LTJjNTE1MTk0ODQxYyIsIk5pY2tOYW1lIjoiMDEyNzcxMjc5MiIsIkF1dGhvcml0eUlkIjoiODg4IiwiZXhwIjoxNzgwOTg2NTg4LCJpc3MiOiJxbVBsdXMiLCJuYmYiOjE3NjU0MzM1ODh9.qENhTNMmr15CPAvrvZ8SIeVRZSNXLtrjcqynwaGqAgI",
        "tips": "",
        "expiresAt": 1780986588000
    },
    "msg": ""}

Case:
url:https://www.qa-my.carsome.com/website/account/consultation/consultation
headers:
{"sec-fetch-mode":"cors","country":"MY","referer":"https://www.qa-my.carsome.com/buy-car/perodua/viva/2014-perodua-viva--1.3/c4b0000","sec-fetch-site":"same-origin","accept-language":"zh-CN,zh;q=0.9,en;q=0.8","origin":"https://www.qa-my.carsome.com",
  "x-token":"Bearer ${token}","x-language":"en","priority":"u=1, i","pragma":"no-cache","accept":"application/json, text/plain, */*","sec-ch-ua":"","sec-ch-ua-mobile":"?0","sec-ch-ua-platform":"","x-platform":"web","content-type":"application/json","cache-control":"no-cache","sec-fetch-dest":"empty","user-agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"}

body:
{"carId":471,"carState":200}
