import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

export default function InscriptionPage() {
  const { connecter } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    motDePasse: '',
    role: 'PATIENT',
  })
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)

  function changer(champ) {
    return (e) => setForm({ ...form, [champ]: e.target.value })
  }

  async function soumettre(e) {
    e.preventDefault()
    setErreur('')
    setEnvoi(true)
    try {
      const { data } = await api.post('/auth/register', form)
      connecter(data)
      navigate(data.role === 'PROFESSIONNEL' ? '/pro' : '/')
    } catch (err) {
      const code = err.response?.status
      if (code === 409) setErreur('Cet email est déjà utilisé')
      else if (code === 400) setErreur('Vérifie les champs (mot de passe : 8 caractères minimum)')
      else setErreur('Impossible de joindre le serveur')
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="page">
      <form className="formulaire" onSubmit={soumettre}>
        <h2>Créer un compte</h2>
        <label>
          Nom
          <input required value={form.nom} onChange={changer('nom')} />
        </label>
        <label>
          Prénom
          <input required value={form.prenom} onChange={changer('prenom')} />
        </label>
        <label>
          Email
          <input type="email" required value={form.email} onChange={changer('email')} />
        </label>
        <label>
          Téléphone
          <input value={form.telephone} onChange={changer('telephone')} />
        </label>
        <label>
          Mot de passe (8 caractères minimum)
          <input
            type="password"
            required
            minLength={8}
            value={form.motDePasse}
            onChange={changer('motDePasse')}
          />
        </label>
        <label>
          Je suis
          <select value={form.role} onChange={changer('role')}>
            <option value="PATIENT">Patient / citoyen</option>
            <option value="PROFESSIONNEL">Professionnel de santé</option>
          </select>
        </label>
        {erreur && <div className="alerte erreur">{erreur}</div>}
        <button className="bouton" type="submit" disabled={envoi}>
          {envoi ? 'Création...' : "S'inscrire"}
        </button>
        <p>
          Déjà un compte ? <Link to="/connexion">Se connecter</Link>
        </p>
      </form>
    </div>
  )
}