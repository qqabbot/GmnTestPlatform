import requests
import json
import sys

BASE_URL = "http://localhost:7777/api"

def create_project(name):
    print(f"Creating project '{name}'...")
    res = requests.post(f"{BASE_URL}/projects", json={"projectName": name, "description": "Temp project for import test"})
    if res.status_code == 200:
        pid = res.json()['id']
        print(f"Project created with ID: {pid}")
        return pid
    else:
        print(f"Failed to create project: {res.text}")
        sys.exit(1)

def verify_import(project_id):
    print("Importing Swagger JSON...")
    
    swagger_content = {
      "swagger": "2.0",
      "info": { "title": "Petstore", "version": "1.0.0" },
      "host": "petstore.swagger.io",
      "basePath": "/v2",
      "paths": {
        "/pet/{petId}": {
          "get": {
            "tags": ["pet"],
            "summary": "Find pet by ID",
            "operationId": "getPetById",
            "parameters": [
              { "name": "petId", "in": "path", "required": True, "type": "integer" }
            ],
            "responses": { "200": { "description": "successful operation" } }
          },
          "post": {
            "tags": ["pet"],
            "summary": "Updates a pet",
            "parameters": [
                { "name": "petId", "in": "path", "required": True, "type": "integer" }
            ],
            "responses": { "200": { "description": "Success" } }
          }
        }
      }
    }
    
    # Call Import API
    # Note: ImportController expects 'content' as a raw string body for JSON import type? 
    # Checking implementation: @RequestBody(required = false) String content
    # And it determines method based on params.
    
    res = requests.post(
        f"{BASE_URL}/import/swagger", 
        params={"projectId": project_id},
        data=json.dumps(swagger_content),
        headers={"Content-Type": "application/json"}
    )
    
    if res.status_code != 200:
        print(f"Import failed: {res.text}")
        sys.exit(1)
        
    print(f"Import success: {res.json()}")
    
    # Verify Data
    verify_modules(project_id)

def verify_modules(project_id):
    print("Verifying Modules and Cases...")
    res = requests.get(f"{BASE_URL}/modules", params={"projectId": project_id})
    modules = res.json()
    
    pet_module = next((m for m in modules if m['moduleName'] == 'pet'), None)
    if not pet_module:
        print("FAIL: Module 'pet' not found.")
        sys.exit(1)
    
    print(f"Module 'pet' found. ID: {pet_module['id']}")
    
    # Verify Cases
    res = requests.get(f"{BASE_URL}/cases", params={"moduleId": pet_module['id']})
    cases = res.json()
    
    print(f"Found {len(cases)} cases in module 'pet'.")
    
    case_names = [c['caseName'] for c in cases]
    if "Find pet by ID" in case_names and "Updates a pet" in case_names:
        print("SUCCESS: Expected cases found.")
    else:
        print(f"FAIL: Expected cases missing. Found: {case_names}")
        sys.exit(1)

    # Verify URL replacement
    for c in cases:
        if "${base_url}" in c['url'] and "${petId}" in c['url']:
             print(f"SUCCESS: URL dynamic parameter verified for '{c['caseName']}': {c['url']}")
        else:
             print(f"WARNING: URL might not be dynamic: {c['url']}")

if __name__ == "__main__":
    pid = create_project("Import Validation Project")
    verify_import(pid)
