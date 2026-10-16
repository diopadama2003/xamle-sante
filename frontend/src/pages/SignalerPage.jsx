import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'
import { SYMPTOMES } from '../symptomes'

export default function SignalerPage() {
  const [quartiers, setQuartiers] = useState([])
  const [symptome, setSymptome] = useState('FIEVRE')
  const [quartier, setQuartier] = useState('')
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)

  useEffect(() => {
    api
      .get('/signalements/quartiers')
      .then(({ data }) => setQuartiers(data))
      .catch(() => setErreur('Impossible de joindre le serveur'))
  }, [])

  async function soumettre(e) {
    e.preventDefault()
    setErreur('')
    setMessage('')
    setEnvoi(true)
    try {
      const { data } = await api.post('/signalements', { symptome, quartier })
      setMessage(data.message)
    } catch (err) {
      const code = err.response?.status
      if (code === 429) setErreur('Trop de signalements. Réessaie plus tard.')
      else if (code === 400) setErreur('Choisis un quartier dans la liste')
      else setErreur('Impossible de joindre le serveur')
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="page">
      <form className="formulaire" onSubmit={soumettre}>
        <h2>Signaler un symptôme</h2>
        <p className="note">
          Ton signalement est anonyme : aucun nom, aucun numéro, aucune position précise n'est
          enregistré. Il est vérifié par un modérateur médical avant d'apparaître sur la carte.
        </p>

        <label>
          Symptôme observé
          <select value={symptome} onChange={(e) => setSymptome(e.target.value)}>
            {SYMPTOMES.map((s) => (
              <option key={s.code} value={s.code}>
                {s.libelle}
              </option>
            ))}
          </select>
        </label>

        <label>
          Quartier
          <select required value={quartier} onChange={(e) => setQuartier(e.target.value)}>
            <option value="">Choisir un quartier</option>
            {quartiers.map((q) => (
              <option key={q} value={q}>
                {q}
              </option>
            ))}
          </select>
        </label>

        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}

        <button className="bouton" type="submit" disabled={envoi}>
          {envoi ? 'Envoi...' : 'Envoyer le signalement'}
        </button>
        <p>
          <Link to="/chaleur">Voir la carte des alertes</Link>
        </p>
      </form>
    </div>
  )
}