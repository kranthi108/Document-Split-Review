## Document Split + Review Backend (Spring Boot)
Backend service to review and adjust AI-generated PDF splits. Stores only metadata (users, splits, documents, pages) in H2; PDFs are not stored. A mock download service returns generated PDFs.

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
- GET `/api/splits/{split_id}` → split with documents and pages
- PATCH `/api/documents/{id}` → update document metadata
- POST `/api/pages/move` → move page IDs to target document
- POST `/api/documents` → create a new document from page IDs
- DELETE `/api/documents/{id}?reassignTo={docId}` → delete; reassign pages or mark unassigned
- POST `/api/splits/{split_id}/finalize` → finalize split
- GET `/api/documents/{id}/download` → mock PDF blob

### Request/Response Examples
- Create document:
```json
POST /api/documents
{
  "splitId": 1,
  "name": "Form 80C",
  "classification": "80C",
  "filename": "client_80c.pdf",
  "pageIds": [5,6,7]
}
```
- Update document:
```json
PATCH /api/documents/10
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
  "targetDocumentId": 10
}
```
- Split response shape:
```json
{
  "id": 1,
  "originalFilename": "bundle.pdf",
  "status": "PENDING",
  "createdAt": "2025-11-11T10:00:00",
  "updatedAt": "2025-11-11T10:00:00",
  "documents": [
    {
      "id": 10,
      "name": "Form 80C",
      "classification": "80C",
      "filename": "client_80c.pdf",
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
- Metrics counters (Micrometer): `api.split.get`, `api.document.create`, `api.document.update`, `api.document.delete`, `api.pages.move`, `api.split.finalize`, `api.document.download`.

### Assumptions
- Users are Chartered Accountants (role `ACCOUNTANT`).
- Service stores only metadata: users, splits, documents, pages; no real PDF storage.
- Splits are created upstream by AI; this service starts post-split. An example creator exists in `SplitService`.
- Deleting a document will:
  - If `reassignTo` is provided, move pages to that document (must be in the same split).
  - Otherwise, mark pages as unassigned (`document_id = null`).
- Validation:
  - CreateDocumentRequest requires `splitId`, `name`, `classification`, `filename`, and non-empty `pageIds`.
  - MovePagesRequest requires non-empty `pageIds` and `targetDocumentId`.
  - UpdateDocumentRequest fields are optional; only provided fields are updated.
- AuthZ: users can access only their own splits/documents.
- Download API returns a generated mock PDF; content does not map to actual metadata.

### Postman
A Postman collection is included at project root: `ascend-doc-split-review.postman_collection.json`. Import it and set the `base_url` (default `http://localhost:8080`) and `token` variables.
