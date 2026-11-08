import { useState } from 'react'
import '../annuaire.css'

// Photo du professionnel, ou ses initiales s'il n'en a pas (ou si l'image ne charge pas)
export default function AvatarMedecin({ medecin, taille = 52 }) {
  const [echec, setEchec] = useState(null)
  const initiales = `${(medecin.prenom || '')[0] || ''}${(medecin.nom || '')[0] || ''}`.toUpperCase()
  const style = { width: taille, height: taille, fontSize: Math.round(taille * 0.38) }

  if (medecin.photoUrl && echec !== medecin.photoUrl) {
    return (
      <img
        className="avatar-medecin"
        src={medecin.photoUrl}
        alt=""
        style={style}
        loading="lazy"
        onError={() => setEchec(medecin.photoUrl)}
      />
    )
  }
  return (
    <span className="avatar-medecin initiales" style={style} aria-hidden="true">
      {initiales || '?'}
    </span>
  )
}