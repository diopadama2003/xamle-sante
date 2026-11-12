import '../flottants.css'

// Icônes médicales dessinées en traits, chacune avec sa couleur
const FORMES = {
  croix: <path d="M9 3h6v6h6v6h-6v6H9v-6H3V9h6z" />,
  coeur: (
    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
  ),
  pouls: <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />,
  pilule: (
    <g transform="rotate(-45 12 12)">
      <rect x="2" y="8" width="20" height="8" rx="4" />
      <line x1="12" y1="8" x2="12" y2="16" />
    </g>
  ),
  thermometre: <path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z" />,
  goutte: <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />,
  seringue: (
    <g transform="rotate(45 12 12)">
      <rect x="9" y="6" width="6" height="10" rx="1" />
      <line x1="12" y1="16" x2="12" y2="21" />
      <line x1="12" y1="2" x2="12" y2="6" />
      <line x1="7" y1="6" x2="17" y2="6" />
      <line x1="9" y1="10" x2="12" y2="10" />
    </g>
  ),
  stethoscope: (
    <>
      <path d="M5 3v6a4 4 0 0 0 8 0V3" />
      <path d="M9 13v1a5 5 0 0 0 10 0v-2" />
      <circle cx="19" cy="10" r="2" />
    </>
  ),
  bouclier: <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />,
}

const BLEU = '#0b5ed7'
const CYAN = '#1dc8ff'
const ROUGE = '#e74c3c'
const ROSE = '#ff6b8b'
const ORANGE = '#ff9f1c'
const VERT = '#22b573'
const VIOLET = '#7c5cff'

// x, y : position en % ; t : taille en px ; d : durée du va-et-vient (s) ; delai : décalage (s)
const ICONES = [
  { f: 'croix', c: ROUGE, x: 3, y: 10, t: 78, d: 7, delai: 0 },
  { f: 'coeur', c: ROSE, x: 30, y: 4, t: 64, d: 9, delai: 1.2 },
  { f: 'pilule', c: ORANGE, x: 44, y: 66, t: 88, d: 8, delai: 0.6 },
  { f: 'stethoscope', c: BLEU, x: 8, y: 56, t: 92, d: 10, delai: 2 },
  { f: 'thermometre', c: VIOLET, x: 54, y: 14, t: 70, d: 8.5, delai: 1.6 },
  { f: 'goutte', c: CYAN, x: 22, y: 80, t: 56, d: 6.5, delai: 0.3 },
  { f: 'pouls', c: VERT, x: 62, y: 78, t: 86, d: 9.5, delai: 2.4 },
  { f: 'seringue', c: BLEU, x: 76, y: 8, t: 82, d: 10.5, delai: 0.9 },
  { f: 'croix', c: CYAN, x: 91, y: 44, t: 62, d: 7.5, delai: 1.8 },
  { f: 'bouclier', c: VERT, x: 72, y: 46, t: 70, d: 11, delai: 0.4 },
  { f: 'coeur', c: ROUGE, x: 3, y: 86, t: 54, d: 8, delai: 2.8 },
  { f: 'goutte', c: BLEU, x: 93, y: 82, t: 64, d: 9, delai: 1.1 },
]

// Le paramètre "variante" n'est plus utilisé : les icônes ont leurs propres couleurs
export default function IconesFlottantes() {
  return (
    <div className="flottants" aria-hidden="true">
      {ICONES.map((ic, i) => (
        <svg
          key={i}
          className="flottant"
          viewBox="0 0 24 24"
          width={ic.t}
          height={ic.t}
          fill={ic.c}
          fillOpacity="0.14"
          stroke={ic.c}
          strokeWidth="1.7"
          strokeLinecap="round"
          strokeLinejoin="round"
          style={{
            left: `${ic.x}%`,
            top: `${ic.y}%`,
            animationDuration: `${ic.d}s`,
            animationDelay: `-${ic.delai}s`,
          }}
        >
          {FORMES[ic.f]}
        </svg>
      ))}
    </div>
  )
}