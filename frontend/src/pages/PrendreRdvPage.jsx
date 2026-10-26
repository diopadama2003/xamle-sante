import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

function formater(iso) {
  return new Date(iso).toLocaleString('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function PrendreRdvPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { deconnecter } = useAuth()
  const [specialiste, setSpecialiste] = useState(null)
  const [creneaux, setCreneaux] = useState([])
  const [choix, setChoix] = useState(null)
  const [motif, setMotif] = useState('')
  const [erreur, setErreur] = useState('')
  const [chargement, setChargement] = useState(true)
  const [envoi, setEnvoi] = useState(false)

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  function charger() {
    return Promise.all([
      api.get(`/specialistes/${id}`),
      api.get(`/specialistes/${id}/creneaux`),
    ])
      .then(([s, c]) => {
        setSpecialiste(s.data)
        setCreneaux(c.data)
      })
      .catch((err) => {
        setErreur(
          err.response?.status === 404
            ? 'Spécialiste introuvable'
            : 'Impossible de joindre le serveur',
        )
      })
      .finally(() => setChargement(false))
  }

  useEffect(() => {
    charger()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function reserver(e) {
    e.preventDefault()
    if (!choix) {
      setErreur('Choisis un créneau')
      return
    }
    setErreur('')
    setEnvoi(true)
    try {
      const { data } = await api.post('/patient/rendez-vous', { creneauId: choix, motif })
      navigate(`/paiement/${data.paiementReference}`)
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) {
        sessionExpiree()
      } else if (code === 409) {
        setErreur("Ce créneau vient d'être réservé. Choisis-en un autre.")
        setChoix(null)
        charger()
      } else if (code === 400) {
        setErreur('Ce créneau est trop proche ou déjà passé')
      } else {
        setErreur('Impossible de joindre le serveur')
      }
    } finally {
      setEnvoi(false)
    }
  }

  if (chargement) return <div className="page">Chargement...</div>

  return (
    <div className="page">
      <form className="formulaire large" onSubmit={reserver}>
        <h2>Prendre rendez-vous</h2>

        {specialiste && (
          <div className="carte-profil">
            <strong>
              Dr {specialiste.prenom} {specialiste.nom}
            </strong>
            <div className="detail">{specialiste.specialite}</div>
            <div className="detail">{specialiste.quartier}</div>
          </div>
        )}

        <p className="note">
          Consultation vidéo. Frais de dossier : 1 000 FCFA, payables par Wave (simulation).
        </p>

        {creneaux.length === 0 && !erreur && (
          <div className="alerte attente">Aucun créneau disponible pour le moment.</div>
        )}

        <div className="creneaux">
          {creneaux.map((c) => (
            <button
              type="button"
              key={c.id}
              className={choix === c.id ? 'bouton' : 'bouton secondaire'}
              onClick={() => setChoix(c.id)}
            >
              {formater(c.debut)}
            </button>
          ))}
        </div>

        <label>
          Motif de la consultation (facultatif)
          <textarea rows={3} maxLength={500} value={motif} onChange={(e) => setMotif(e.target.value)} />
        </label>

        {erreur && <div className="alerte erreur">{erreur}</div>}

        <button className="bouton" type="submit" disabled={envoi || creneaux.length === 0}>
          {envoi ? 'Réservation...' : 'Réserver et payer'}
        </button>
        <p>
          <Link to="/">Retour à la carte</Link>
        </p>
      </form>
    </div>
  )
}