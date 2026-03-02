CREATE TABLE IF NOT EXISTS departments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  slug text NOT NULL UNIQUE,
  name text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS blackboard_documents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  department_id uuid NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
  doc_key text NOT NULL,
  title text NOT NULL,
  required_headings text[] NOT NULL DEFAULT '{}',
  current_version_id uuid NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (department_id, doc_key)
);

CREATE TABLE IF NOT EXISTS blackboard_versions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id uuid NOT NULL REFERENCES blackboard_documents(id) ON DELETE CASCADE,
  version_no integer NOT NULL,
  markdown text NOT NULL,
  markdown_sha256 text NOT NULL,
  headings text[] NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now(),
  source text NOT NULL,
  UNIQUE (document_id, version_no),
  UNIQUE (document_id, markdown_sha256)
);

ALTER TABLE blackboard_documents
  ADD CONSTRAINT blackboard_documents_current_version_fk
  FOREIGN KEY (current_version_id) REFERENCES blackboard_versions(id);
