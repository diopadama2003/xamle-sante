import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'
import { libelleSymptome } from '../symptomes'

export default function ModerationPage() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [profils, setProfils] = useState([])
  const [signalements, setSignalements] = useState([])
  const [chargement, setChargement] = useState(true)
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  useEffect(() => {
    Promise.all([
      api.get('/moderation/specialistes/en-attente'),
      api.get('/moderation/signalements/en-attente'),
    ])
      .then(([p, s]) => {
        setProfils(p.data)
        setSignalements(s.data)
        setErreur('')
      })
      .catch((err) => {
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else setErreur('Impossible de joindre le serveur')
      })
      .finally(() => setChargement(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function deciderProfil(profil, action) {
    setMessage('')
    try {
      await api.patch(`/moderation/specialistes/${profil.id}/${action}`)
      setProfils((liste) => liste.filter((p) => p.id !== profil.id))
      setMessage(
        action === 'valider'
          ? `Dr ${profil.prenom} ${profil.nom} validé : profil public.`
          : `Dr ${profil.prenom} ${profil.nom} rejeté.`,
      )
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else setErreur("L'action a échoué")
    }
  }

  async function deciderSignalement(sig, action) {
    setMessage('')
    try {
      await api.patch(`/moderation/signalements/${sig.id}/${action}`)
      setSignalements((liste) => liste.filter((s) => s.id !== sig.id))
      setMessage(
        action === 'valider'
          ? 'Signalement validé : il compte dans la carte de chaleur.'
          : 'Signalement rejeté.',
      )
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else setErreur("L'action a échoué")
    }
  }

  if (chargement) return <div className="page">Chargement...</div>

  return (
    <div className="page">
      <div className="formulaire large">
        <h2>Profils à valider</h2>

        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}

        {profils.length === 0 && !erreur && (
          <div className="alerte ok">Aucun profil en attente.</div>
        )}

        {profils.map((p) => (
          <div key={p.id} className="carte-profil">
            <div>
              <strong>
                Dr {p.prenom} {p.nom}
              </strong>{' '}
              · {p.specialite}
            </div>
            <div className="detail">
              {p.quartier || 'Quartier non renseigné'} · {p.telephone || 'Sans téléphone'}
            </div>
            <div className="detail">Langues : {p.langues || 'non renseignées'}</div>
            <div className="detail">
              Position : {p.latitude}, {p.longitude}
            </div>
            <div className="actions">
              <button className="bouton" onClick={() => deciderProfil(p, 'valider')}>
                Valider
              </button>
              <button className="bouton danger" onClick={() => deciderProfil(p, 'rejeter')}>
                Rejeter
              </button>
            </div>
          </div>
        ))}

        <h2>Signalements à vérifier</h2>

        {signalements.length === 0 && !erreur && (
          <div className="alerte ok">Aucun signalement en attente.</div>
        )}

        {signalements.map((s) => (
          <div key={s.id} className="carte-profil">
            <div>
              <strong>{libelleSymptome(s.symptome)}</strong> · {s.quartier}
            </div>
            <div className="detail">
              Reçu le {new Date(s.dateCreation).toLocaleString('fr-FR')}
            </div>
            <div className="actions">
              <button className="bouton" onClick={() => deciderSignalement(s, 'valider')}>
                Valider
              </button>
              <button className="bouton danger" onClick={() => deciderSignalement(s, 'rejeter')}>
                Rejeter
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}