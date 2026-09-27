import { useCallback, useState } from "react";
import { Alert, Badge, Button, Card, Col, ListGroup, Row } from "react-bootstrap";

import LoadingSpinner from "../../components/LoadingSpinner";
import { useApi } from "../../hooks/useApi";
import {
  TYPE_BADGES,
  TYPE_LABELS,
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from "../../services/notificationService";

function formatDate(value) {
  if (!value) return "";
  const d = new Date(value);
  return d.toLocaleString();
}

export default function NotificationsPage() {
  const { data, loading, error, refetch } = useApi(
    () => fetchNotifications({ unread_first: true }),
    []
  );
  const { data: unread } = useApi(() => fetchUnreadCount(), []);
  const [viewing, setViewing] = useState(null);

  const markRead = useCallback(
    (id) => {
      markNotificationRead(id).then(refetch);
      setViewing(null);
    },
    [refetch]
  );

  const results = data?.results || [];

  return (
    <Card>
      <Card.Header className="d-flex justify-content-between align-items-center">
        <span>
          Notifications
          {unread && unread.count > 0 && (
            <Badge bg="danger" className="ms-2">{unread.count} unread</Badge>
          )}
        </span>
        {data && data.count > 0 && (
          <Button size="sm" variant="outline-primary" onClick={() => markAllNotificationsRead().then(refetch)}>
            Mark all read
          </Button>
        )}
      </Card.Header>
      <Card.Body>
        {error && <Alert variant="danger">{error}</Alert>}
        {loading && <LoadingSpinner />}
        {!loading && results.length === 0 && (
          <Alert variant="light" className="text-muted mb-0">No notifications</Alert>
        )}
        <Row>
          <Col lg={viewing === null ? 12 : 8}>
            <ListGroup>
              {results.map((n) => (
                <ListGroup.Item
                  key={n.id}
                  action
                  onClick={() => setViewing(n.id)}
                  className="d-flex justify-content-between align-items-center"
                >
                  <div className="text-truncate me-2">
                    <Badge bg="" className={`me-2 ${TYPE_BADGES[n.type] || TYPE_BADGES.SYSTEM}`}>
                      {TYPE_LABELS[n.type] || n.type}
                    </Badge>
                    <span className={n.is_read ? "text-muted" : "fw-semibold"}>{n.message}</span>
                  </div>
                  <div className="d-flex align-items-center gap-2 flex-shrink-0">
                    <small className="text-muted text-nowrap">{formatDate(n.created_at)}</small>
                    {!n.is_read && (
                      <Button size="sm" variant="outline-success" onClick={(e) => { e.stopPropagation(); markRead(n.id); }}>
                        Mark read
                      </Button>
                    )}
                  </div>
                </ListGroup.Item>
              ))}
            </ListGroup>
            {data && data.count > results.length && (
              <div className="text-muted small mt-3">Showing {results.length} of {data.count}</div>
            )}
          </Col>
          {viewing !== null && (() => {
            const n = results.find((x) => x.id === viewing);
            if (!n) return null;
            return (
              <Col lg={4}>
                <Card className="h-100">
                  <Card.Header>Details</Card.Header>
                  <Card.Body>
                    <Row className="mb-2">
                      <Col xs={5} className="text-muted">Type</Col>
                      <Col><Badge bg="" className={TYPE_BADGES[n.type]}>{TYPE_LABELS[n.type] || n.type}</Badge></Col>
                    </Row>
                    <Row className="mb-2">
                      <Col xs={5} className="text-muted">Message</Col>
                      <Col>{n.message}</Col>
                    </Row>
                    <Row className="mb-2">
                      <Col xs={5} className="text-muted">Received</Col>
                      <Col>{formatDate(n.created_at)}</Col>
                    </Row>
                    <div className="mt-3">
                      {!n.is_read && (
                        <Button size="sm" variant="success" onClick={() => markRead(n.id)}>Mark as read</Button>
                      )}
                    </div>
                  </Card.Body>
                </Card>
              </Col>
            );
          })()}
        </Row>
      </Card.Body>
    </Card>
  );
}