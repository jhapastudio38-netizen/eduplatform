/**
 * Audit log helper — every admin/teacher mutating action should be recorded.
 */
import { db } from "@/lib/db";

export async function audit(params: {
  actorId?: string;
  action: string;
  entity: string;
  entityId?: string;
  metadata?: Record<string, unknown>;
  ip?: string;
}) {
  try {
    await db.auditLog.create({
      data: {
        actorId: params.actorId,
        action: params.action,
        entity: params.entity,
        entityId: params.entityId,
        metadata: params.metadata ? JSON.stringify(params.metadata) : null,
        ip: params.ip,
      },
    });
  } catch (e) {
    // Audit failures must NEVER break the request
    console.error("[audit] failed", e);
  }
}
