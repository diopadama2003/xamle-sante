import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

export default function ModerationPage() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [profils, setProfils] = useState([])
  const [chargement, setChargement] = useState(true)
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  function charger() {
    api
      .get('/moderation/specialistes/en-attente')
      .then(({ data }) => {
        setProfils(data)
        setErreur('')
      })
      .catch((err) => {
        const code = err.response?.status
        if (code === 401 || code === 403) sessionExpiree()
        else setErreur('Impossible de joindre le serveur')
      })
      .finally(() => setChargement(false))
  }

  useEffect(() => {
    charger()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function decider(profil, action) {
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
              <button className="bouton" onClick={() => decider(p, 'valider')}>
                Valider
              </button>
              <button className="bouton danger" onClick={() => decider(p, 'rejeter')}>
                Rejeter
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}