## Document Split + Review Backend (Spring Boot)
Backend service to review and adjust AI-generated PDF splits. Stores only metadata (users, original documents, split parts, pages) in H2; PDFs are not stored. A mock download service returns generated PDFs.

### Run locally
- Prereqs: Java 17, Maven
- Start:
```bash
cd ascend-doc-split-review
./mvnw spring-boot:run
```
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:testdb`, user `sa`, pass `password`)
- Actuator: `GET /actuator/health`, `GET /actuator/metrics`

### Auth
- JWT-based. Register or login to obtain a token, then pass `Authorization: Bearer <token>` on all API calls.
- Endpoints:
  - POST `/api/auth/register` { username, password, email }
  - POST `/api/auth/login` { username, password }

### Core APIs
- GET `/api/documents/{documentId}` → original document with split parts and pages
- GET `/api/splits/{id}` → get a split part by id
- POST `/api/split-parts` → create a new split part from page IDs of the same original document
- POST `/api/document` → alias for creating a split part (document) from page IDs
- PATCH `/api/split-parts/{id}` → update split part metadata
- PATCH `/api/document/{id}` → alias for updating split part (document) metadata
- DELETE `/api/split-parts/{id}?reassignTo={splitPartId}` → delete; reassign pages to another split part (same original doc) or mark unassigned
- DELETE `/api/document/{id}?reassignTo={splitPartId}` → alias for deleting a split part (document)
- POST `/api/pages/move` → move page IDs to a target split part (must be same original document)
- POST `/api/documents/{documentId}/finalize` → finalize original document (lock further changes)
- POST `/api/split-parts/{id}/finalize` → finalize a split part (lock further changes)
- GET `/api/documents/{id}/download` → mock PDF blob

### Request/Response Examples
- Create split part:
```json
POST /api/split-parts
{
  "originalDocumentId": 1,
  "name": "Form 80C",
  "classification": "80C",
  "filename": "client_80c.pdf",
  "pageIds": [5,6,7]
}
```
- Create document (alias):
```json
POST /api/document
{
  "originalDocumentId": 1,
  "name": "Form 80C",
  "classification": "80C",
  "filename": "client_80c.pdf",
  "pageIds": [5,6,7]
}
```
- Update split part:
```json
PATCH /api/split-parts/10
{
  "name": "Form 80D",
  "classification": "80D",
  "filename": "updated_80d.pdf"
}
```
- Update document (alias):
```json
PATCH /api/document/10
{
  "name": "Form 80D",
  "classification": "80D",
  "filename": "updated_80d.pdf"
}
```
- Move pages:
```json
POST /api/pages/move
{
  "pageIds": [8,9],
  "targetSplitPartId": 10
}
```
- Original document response shape:
```json
{
  "id": 1,
  "originalFilename": "bundle.pdf",
  "status": "PENDING",
  "createdAt": "2025-11-11T10:00:00",
  "updatedAt": "2025-11-11T10:00:00",
  "splitParts": [
    {
      "id": 10,
      "name": "Form 80C",
      "classification": "80C",
      "filename": "client_80c.pdf",
      "fromPage": 1,
      "toPage": 2,
      "createdAt": "2025-11-11T10:00:00",
      "updatedAt": "2025-11-11T10:00:00",
      "pages": [
        { "id": 5, "pageNumber": 1, "content": "..." }
      ]
    }
  ]
}
```

### Observability
- Logs include user and entity IDs on key operations.
- Metrics counters (Micrometer): `api.document.get`, `api.splitpart.create`, `api.splitpart.update`, `api.splitpart.delete`, `api.pages.move`, `api.document.finalize`, `api.document.download`.

### Assumptions
- Users are Chartered Accountants (role `ACCOUNTANT`).
- Service stores only metadata: users, original documents, split parts, pages; no real PDF storage.
- Original documents are created upstream by AI; this service manages split parts and page moves. An example creator exists in `OriginalDocumentService` and the `DataSeeder`.
- Deleting a split part will:
  - If `reassignTo` is provided, move pages to that split part (must be in the same original document).
  - Otherwise, mark pages as unassigned (`split_part_id = null`).
- Validation:
  - CreateSplitPartRequest requires `originalDocumentId`, `name`, `classification`, `filename`, and non-empty `pageIds`.
  - MovePagesRequest requires non-empty `pageIds` and `targetSplitPartId`.
  - UpdateSplitPartRequest fields are optional; only provided fields are updated.
- AuthZ: users can access only their own original documents and split parts.
- Download API returns a generated mock PDF; content does not map to actual metadata.
- Finalization rules:
  - When an original document is finalized, no creates/updates/moves/deletes are allowed within it.
  - When a split part is finalized, it cannot be modified and pages cannot be moved into or out of it.

### Postman
A Postman collection is included at project root: `ascend-doc-split-review.postman_collection.json`. Import it and set the `base_url` (default `http://localhost:8080`) and `token` variables.

### Seeded demo data
- User: username `demo`, password `password`
- On startup, one original document is created with two split parts:
  - `Form 80C` (classification `80C`) with pages 1–2
  - `Form 80D` (classification `80D`) with page 3
- IDs are assigned at runtime; to find exact IDs:
  - Check application logs on startup (seed logs print ids), or
  - Use H2 console:
    - `SELECT id FROM original_documents;`
    - `SELECT id, original_document_id, name FROM split_parts;`
