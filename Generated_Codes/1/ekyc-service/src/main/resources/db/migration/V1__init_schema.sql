-- Create schema
CREATE SCHEMA IF NOT EXISTS ekyc;

-- Set search path
SET search_path TO ekyc;

-- Create enum types
CREATE TYPE verification_status AS ENUM ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'EXPIRED');
CREATE TYPE document_type AS ENUM ('PASSPORT', 'NATIONAL_ID', 'DRIVING_LICENSE', 'RESIDENCE_PERMIT');
CREATE TYPE verification_step AS ENUM ('DOCUMENT_UPLOAD', 'FACE_VERIFICATION', 'LIVENESS_CHECK', 'DOCUMENT_VERIFICATION', 'BACKGROUND_CHECK');
CREATE TYPE step_status AS ENUM ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'SKIPPED');

-- Create customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    nationality VARCHAR(50),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create verification_requests table
CREATE TABLE verification_requests (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    reference_id VARCHAR(100) NOT NULL UNIQUE,
    status verification_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    callback_url VARCHAR(255),
    metadata JSONB
);

-- Create documents table
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    verification_request_id UUID NOT NULL REFERENCES verification_requests(id),
    document_type document_type NOT NULL,
    document_number VARCHAR(100),
    issuing_country VARCHAR(50),
    issue_date DATE,
    expiry_date DATE,
    front_image_url VARCHAR(255),
    back_image_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_status verification_status NOT NULL DEFAULT 'PENDING',
    verification_details JSONB
);

-- Create verification_steps table
CREATE TABLE verification_steps (
    id UUID PRIMARY KEY,
    verification_request_id UUID NOT NULL REFERENCES verification_requests(id),
    step verification_step NOT NULL,
    status step_status NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    failure_reason TEXT,
    result_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (verification_request_id, step)
);

-- Create face_verifications table
CREATE TABLE face_verifications (
    id UUID PRIMARY KEY,
    verification_request_id UUID NOT NULL REFERENCES verification_requests(id),
    selfie_image_url VARCHAR(255),
    liveness_video_url VARCHAR(255),
    similarity_score DECIMAL(5,2),
    liveness_score DECIMAL(5,2),
    verification_status verification_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_details JSONB
);

-- Create audit_logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(100),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSONB
);

-- Create indexes
CREATE INDEX idx_verification_requests_customer_id ON verification_requests(customer_id);
CREATE INDEX idx_verification_requests_status ON verification_requests(status);
CREATE INDEX idx_documents_verification_request_id ON documents(verification_request_id);
CREATE INDEX idx_verification_steps_verification_request_id ON verification_steps(verification_request_id);
CREATE INDEX idx_face_verifications_verification_request_id ON face_verifications(verification_request_id);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_customers_updated_at
BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_verification_requests_updated_at
BEFORE UPDATE ON verification_requests
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_verification_steps_updated_at
BEFORE UPDATE ON verification_steps
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_face_verifications_updated_at
BEFORE UPDATE ON face_verifications
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();