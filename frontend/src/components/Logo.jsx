export default function Logo({ taille = 34 }) {
  return (
    <svg width={taille} height={taille} viewBox="0 0 40 40" aria-hidden="true">
      <rect width="40" height="40" rx="10" fill="#0b7a5a" />
      <path d="M17 8h6v9h9v6h-9v9h-6v-9H8v-6h9z" fill="#fff" />
      <polyline
        points="9,20 15,20 18,14 22,27 25,20 31,20"
        fill="none"
        stroke="#0b7a5a"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}