import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

const TYPES = {
  VACCINATION: 'Vaccination',
  DEPISTAGE: 'Dépistage',
  SENSIBILISATION: 'Sensibilisation',
}

const VIDE = {
  titre: '',
  type: 'VACCINATION',
  description: '',
  lieu: '',
  quartier: '',
  dateDebut: '',
  dateFin: '',
}

function formater(iso) {
  return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AdminCampagnesPage() {
  const { deconnecter } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(VIDE)
  const [campagnes, setCampagnes] = useState([])
  const [quartiers, setQuartiers] = useState([])
  const [message, setMessage] = useState('')
  const [erreur, setErreur] = useState('')

  function sessionExpiree() {
    deconnecter()
    navigate('/connexion')
  }

  function charger() {
    api
      .get('/campagnes')
      .then(({ data }) => setCampagnes(data))
      .catch(() => setErreur('Impossible de joindre le serveur'))
  }

  useEffect(() => {
    charger()
    api
      .get('/signalements/quartiers')
      .then(({ data }) => setQuartiers(data))
      .catch(() => {})
  }, [])

  function changer(champ) {
    return (e) => setForm({ ...form, [champ]: e.target.value })
  }

  async function creer(e) {
    e.preventDefault()
    setErreur('')
    setMessage('')
    try {
      await api.post('/admin/campagnes', form)
      setForm(VIDE)
      setMessage('Campagne créée.')
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else if (code === 400) setErreur('Vérifie les champs (la date de fin doit suivre le début)')
      else setErreur('Impossible de joindre le serveur')
    }
  }

  async function supprimer(c) {
    if (!window.confirm(`Supprimer « ${c.titre} » ?`)) return
    setErreur('')
    setMessage('')
    try {
      await api.delete(`/admin/campagnes/${c.id}`)
      setMessage('Campagne supprimée.')
      charger()
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else setErreur('La suppression a échoué')
    }
  }

  async function lancerRappels() {
    setErreur('')
    setMessage('')
    try {
      const { data } = await api.post('/admin/campagnes/rappels/executer')
      setMessage(`${data.rappelsEnvoyes} rappel(s) envoyé(s).`)
    } catch (err) {
      const code = err.response?.status
      if (code === 401 || code === 403) sessionExpiree()
      else setErreur("L'envoi des rappels a échoué")
    }
  }

  return (
    <div className="page">
      <form className="formulaire large" onSubmit={creer}>
        <h2>Nouvelle campagne</h2>

        <label>
          Titre
          <input required value={form.titre} onChange={changer('titre')} />
        </label>
        <label>
          Type
          <select value={form.type} onChange={changer('type')}>
            {Object.entries(TYPES).map(([code, libelle]) => (
              <option key={code} value={code}>
                {libelle}
              </option>
            ))}
          </select>
        </label>
        <label>
          Description
          <textarea rows={3} value={form.description} onChange={changer('description')} />
        </label>
        <label>
          Lieu
          <input required value={form.lieu} onChange={changer('lieu')} />
        </label>
        <label>
          Quartier
          <select required value={form.quartier} onChange={changer('quartier')}>
            <option value="">Choisir un quartier</option>
            {quartiers.map((q) => (
              <option key={q} value={q}>
                {q}
              </option>
            ))}
          </select>
        </label>
        <label>
          Début
          <input
            type="datetime-local"
            required
            value={form.dateDebut}
            onChange={changer('dateDebut')}
          />
        </label>
        <label>
          Fin
          <input
            type="datetime-local"
            required
            value={form.dateFin}
            onChange={changer('dateFin')}
          />
        </label>

        {message && <div className="alerte ok">{message}</div>}
        {erreur && <div className="alerte erreur">{erreur}</div>}

        <button className="bouton" type="submit">
          Créer la campagne
        </button>
      </form>

      <div className="formulaire large" style={{ marginTop: 16 }}>
        <h2>Campagnes à venir</h2>
        <button type="button" className="bouton secondaire" onClick={lancerRappels}>
          Envoyer les rappels maintenant (test)
        </button>

        {campagnes.length === 0 && <div className="alerte attente">Aucune campagne à venir.</div>}

        {campagnes.map((c) => (
          <div key={c.id} className="carte-profil">
            <div>
              <span className="badge">{TYPES[c.type]}</span> <strong>{c.titre}</strong>
            </div>
            <div className="detail">
              {formater(c.dateDebut)} · {c.lieu}
            </div>
            <div className="actions">
              <button className="bouton danger" onClick={() => supprimer(c)}>
                Supprimer
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}