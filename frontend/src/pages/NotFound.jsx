import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import "./NotFound.css";

const QUIPS = [
  "This stock entry seems to have walked out of the warehouse.",
  "Even our best delivery route couldn't find this page.",
  "The ledger shows zero trace of this URL.",
  "This venture doesn't exist on any map we own.",
];

const QUICK_LINKS = [
  { label: "Dashboard", path: "/", icon: "bi-speedometer2" },
  { label: "Ventures", path: "/ventures", icon: "bi-building" },
  { label: "Sales", path: "/sales", icon: "bi-cart-check" },
  { label: "Inventory", path: "/inventory", icon: "bi-clipboard-data" },
];

function NotFound() {
  const location = useLocation();
  const navigate = useNavigate();
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const [quipIndex, setQuipIndex] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setQuipIndex((i) => (i + 1) % QUIPS.length);
    }, 4000);
    return () => clearInterval(timer);
  }, []);

  const stars = useMemo(
    () =>
      Array.from({ length: 26 }, (_, i) => ({
        id: i,
        left: `${(i * 37.7) % 100}%`,
        top: `${(i * 53.3) % 100}%`,
        size: 8 + ((i * 7) % 10),
        delay: `${-((i * 0.37) % 2.6).toFixed(2)}s`,
      })),
    []
  );

  const handleMouse = (event) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = (event.clientX - rect.left) / rect.width - 0.5;
    const y = (event.clientY - rect.top) / rect.height - 0.5;
    setTilt({ x, y });
  };

  return (
    <div
      className="nf-stage text-center text-white px-3 py-5"
      onMouseMove={handleMouse}
      onMouseLeave={() => setTilt({ x: 0, y: 0 })}
    >
      <div className="nf-orb nf-orb-1" />
      <div className="nf-orb nf-orb-2" />
      <div className="nf-orb nf-orb-3" />
      {stars.map((s) => (
        <i
          key={s.id}
          className="bi bi-stars nf-star"
          style={{
            left: s.left,
            top: s.top,
            fontSize: s.size,
            animationDelay: s.delay,
          }}
        />
      ))}

      <div className="position-relative">
        <div className="nf-crate mb-1">
          <i className="bi bi-box-seam" />
        </div>
        <div
          className="nf-digits"
          style={{
            transform: `perspective(700px) rotateY(${tilt.x * 14}deg) rotateX(${
              -tilt.y * 14
            }deg)`,
            transition: "transform 0.12s ease-out",
          }}
        >
          <span className="nf-digit">4</span>
          <span className="nf-digit">0</span>
          <span className="nf-digit">4</span>
        </div>

        <h2 className="fw-bold mt-2">Lost in the warehouse?</h2>
        <p className="text-white-50 mb-1" key={quipIndex}>
          {QUIPS[quipIndex]}
        </p>
        <p className="mb-4">
          <span className="badge nf-path px-3 py-2">{location.pathname}</span>
        </p>

        <div className="d-flex flex-wrap justify-content-center gap-2 mb-4">
          <button
            className="btn btn-light fw-semibold"
            type="button"
            onClick={() => navigate(-1)}
          >
            <i className="bi bi-arrow-left me-1" />
            Go back
          </button>
          <Link to="/" className="btn btn-warning fw-semibold">
            <i className="bi bi-speedometer2 me-1" />
            Back to Dashboard
          </Link>
        </div>

        <div className="d-flex flex-wrap justify-content-center gap-2">
          {QUICK_LINKS.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className="btn btn-sm btn-outline-light nf-link-btn"
            >
              <i className={`bi ${link.icon} me-1`} />
              {link.label}
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}

export default NotFound;
