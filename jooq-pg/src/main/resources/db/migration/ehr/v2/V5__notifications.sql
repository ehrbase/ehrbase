-- EHRbase v2: Change Notifications
-- LISTEN/NOTIFY for real-time composition events
-- PostgreSQL 18+ required

SET search_path TO ehr_system, ext;

-- ============================================================
-- Composition change notification function
-- Fires LISTEN/NOTIFY on INSERT/UPDATE to composition table
-- Subscribers (Spring for GraphQL, external systems) can listen
-- ============================================================
CREATE FUNCTION ehr_system.notify_composition_change() RETURNS trigger AS $$
BEGIN
    PERFORM pg_notify('ehrbase:composition_change', json_build_object(
        'action', TG_OP,
        'ehr_id', NEW.ehr_id,
        'composition_id', NEW.id,
        'template_name', NEW.template_name,
        'sys_version', NEW.sys_version,
        'change_type', NEW.change_type,
        'committed_at', NEW.committed_at
    )::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER composition_change_notify
    AFTER INSERT OR UPDATE ON ehr_system.composition
    FOR EACH ROW EXECUTE FUNCTION ehr_system.notify_composition_change();

-- ============================================================
-- Audit event notification (for SIEM / compliance monitoring)
-- ============================================================
CREATE FUNCTION ehr_system.notify_audit_event() RETURNS trigger AS $$
BEGIN
    PERFORM pg_notify('ehrbase:audit_event', json_build_object(
        'event_type', NEW.event_type,
        'target_type', NEW.target_type,
        'target_id', NEW.target_id,
        'action', NEW.action,
        'actor_id', NEW.actor_id,
        'created_at', NEW.created_at
    )::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_event_notify
    AFTER INSERT ON ehr_system.audit_event
    FOR EACH ROW EXECUTE FUNCTION ehr_system.notify_audit_event();
