# API Endpoints

Base URL: `http://localhost:7777/api`

## Project Management

### Get All Projects
- **URL**: `/projects`
- **Method**: `GET`
- **Response**: Array of projects

### Get Project by ID
- **URL**: `/projects/{id}`
- **Method**: `GET`
- **Response**: Project details

### Create Project
- **URL**: `/projects`
- **Method**: `POST`
- **Body**:
```json
{
  "projectName": "用户服务",
  "description": "用户管理相关API测试"
}
```

### Update Project
- **URL**: `/projects/{id}`
- **Method**: `PUT`
- **Body**: Same as Create

### Delete Project
- **URL**: `/projects/{id}`
- **Method**: `DELETE`

---

## Module Management

### Get All Modules
- **URL**: `/modules`
- **Method**: `GET`
- **Response**: Array of modules

### Get Module by ID
- **URL**: `/modules/{id}`
- **Method**: `GET`
- **Response**: Module details

### Create Module
- **URL**: `/modules`
- **Method**: `POST`
- **Body**:
```json
{
  "moduleName": "用户注册登录",
  "project": {"id": 1}
}
```

### Update Module
- **URL**: `/modules/{id}`
- **Method**: `PUT`
- **Body**: Same as Create

### Delete Module
- **URL**: `/modules/{id}`
- **Method**: `DELETE`

---

## Test Case Management

### Get All Test Cases
- **URL**: `/cases`
- **Method**: `GET`
- **Query Params**:
  - `moduleId` (optional): Filter by module ID
- **Response**: Array of test cases

### Get Test Case by ID
- **URL**: `/cases/{id}`
- **Method**: `GET`
- **Response**: Test case details with steps

### Create Test Case
- **URL**: `/cases`
- **Method**: `POST`
- **Body**:
```json
{
  "caseName": "用户登录测试",
  "method": "POST",
  "url": "${base_url}/users/login",
  "body": "{\"username\": \"admin\", \"password\": \"123456\"}",
  "assertionScript": "status_code == 200",
  "module": {"id": 1}
}
```

### Update Test Case
- **URL**: `/cases/{id}`
- **Method**: `PUT`
- **Body**: Same as Create

### Delete Test Case
- **URL**: `/cases/{id}`
- **Method**: `DELETE`

### Dry Run Test Case
- **URL**: `/cases/{id}/dry-run`
- **Method**: `POST`
- **Body**:
```json
{
  "envKey": "dev"
}
```
- **Response**:
```json
{
  "resolvedUrl": "https://api-dev.example.com/users/login",
  "resolvedBody": "{\"username\": \"admin\", \"password\": \"123456\"}",
  "resolvedHeaders": {},
  "variables": {
    "base_url": "https://api-dev.example.com"
  }
}
```

---

## Environment Management

### Get All Environments
- **URL**: `/environments`
- **Method**: `GET`
- **Response**: Array of environments

### Get Environment by ID
- **URL**: `/environments/{id}`
- **Method**: `GET`
- **Response**: Environment details

### Create Environment
- **URL**: `/environments`
- **Method**: `POST`
- **Body**:
```json
{
  "envName": "dev",
  "description": "开发环境",
  "domain": "https://api-dev.example.com"
}
```

### Update Environment
- **URL**: `/environments/{id}`
- **Method**: `PUT`
- **Body**: Same as Create

### Delete Environment
- **URL**: `/environments/{id}`
- **Method**: `DELETE`

---

## Global Variables

### Get Variables by Environment
- **URL**: `/variables/environment/{envId}`
- **Method**: `GET`
- **Response**: Array of variables

### Create Variable
- **URL**: `/variables`
- **Method**: `POST`
- **Body**:
```json
{
  "varKey": "base_url",
  "varValue": "https://api-dev.example.com",
  "varType": "STRING",
  "description": "基础URL",
  "environment": {"id": 1}
}
```

### Update Variable
- **URL**: `/variables/{id}`
- **Method**: `PUT`
- **Body**: Same as Create

### Delete Variable
- **URL**: `/variables/{id}`
- **Method**: `DELETE`

---

## Test Execution

### Execute Test Cases
- **URL**: `/cases/execute`
- **Method**: `POST`
- **Query Params**:
  - `envKey` (required): Environment key (e.g., `dev`, `test`, `prod`)
  - `projectId` (optional): Execute all cases in project
  - `moduleId` (optional): Execute all cases in module
- **Response**: Array of execution results
```json
[
  {
    "caseName": "用户登录测试",
    "status": "PASS",
    "duration": 245,
    "detail": "Execution logs...",
    "request": {...},
    "response": {...}
  }
]
```

### Execute Groovy Script
- **URL**: `/cases/groovy`
- **Method**: `POST`
- **Body**:
```json
{
  "script": "def result = 1 + 1; return result"
}
```

---

## Test Reports

### Get All Reports
- **URL**: `/reports`
- **Method**: `GET`
- **Response**: Array of execution logs

### Get Reports by Project
- **URL**: `/reports/project/{projectId}`
- **Method**: `GET`
- **Response**: Array of execution logs for project

### Get Reports by Module
- **URL**: `/reports/module/{moduleId}`
- **Method**: `GET`
- **Response**: Array of execution logs for module

---

## Metrics & Monitoring

### Health Check
- **URL**: `/actuator/health`
- **Method**: `GET`
- **Response**: Application health status

### Metrics
- **URL**: `/actuator/metrics`
- **Method**: `GET`
- **Response**: Available metrics list

### Prometheus Metrics
- **URL**: `/actuator/prometheus`
- **Method**: `GET`
- **Response**: Metrics in Prometheus format

---

## Notes

### Variable Substitution
- Use `${variable_name}` syntax in URL, headers, and body
- Variables are resolved from: Environment → Project → Module (hierarchical inheritance)
- Supports SpEL expressions: `${T(System).currentTimeMillis()}`

### Authentication
- Currently no authentication required (to be added in Phase 3.2)

### Error Codes
- `200`: Success
- `400`: Bad Request
- `404`: Not Found
- `405`: Method Not Allowed
- `500`: Internal Server Error
