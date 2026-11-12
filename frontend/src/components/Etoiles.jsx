import { useState } from 'react'
import '../avis.css'

// Note moyenne en lecture seule : étoiles partiellement remplies, chiffre et nombre d'avis
export default function Etoiles({ note, nombre = 0, petit = false }) {
  if (!nombre) {
    return <span className={`avis-aucun ${petit ? 'petit' : ''}`}>Pas encore d'avis</span>
  }
  const valeur = Number(note) || 0
  return (
    <span className={`avis-resume ${petit ? 'petit' : ''}`} title={`${valeur.toFixed(1)} sur 5`}>
      <span className="etoiles" style={{ '--pct': `${(valeur / 5) * 100}%` }} aria-hidden="true">
        ★★★★★
      </span>
      <strong>{valeur.toFixed(1)}</strong>
      <span className="avis-nb">({nombre})</span>
    </span>
  )
}

// Choix de la note : 5 étoiles à toucher
export function ChoixEtoiles({ valeur, onChange }) {
  const [survol, setSurvol] = useState(0)
  const affichee = survol || valeur
  return (
    <div
      className="avis-choix"
      role="radiogroup"
      aria-label="Note de 1 à 5 étoiles"
      onMouseLeave={() => setSurvol(0)}
    >
      {[1, 2, 3, 4, 5].map((i) => (
        <button
          key={i}
          type="button"
          role="radio"
          aria-checked={valeur === i}
          aria-label={`${i} étoile${i > 1 ? 's' : ''}`}
          className={i <= affichee ? 'plein' : ''}
          onMouseEnter={() => setSurvol(i)}
          onClick={() => onChange(i)}
        >
          ★
        </button>
      ))}
      <span className="avis-choix-texte">{valeur ? `${valeur} / 5` : 'Touche une étoile'}</span>
    </div>
  )
}