import { useEffect } from 'react'
import { Routes, Route, useLocation } from 'react-router-dom'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import RouteProtegee from './components/RouteProtegee'
import HomePage from './pages/HomePage'
import CartePage from './pages/CartePage'
import EtablissementsPage from './pages/EtablissementsPage'
import ConnexionPage from './pages/ConnexionPage'
import InscriptionPage from './pages/InscriptionPage'
import EspacePro from './pages/EspacePro'
import ModerationPage from './pages/ModerationPage'
import SosPage from './pages/SosPage'
import ChaleurPage from './pages/ChaleurPage'
import SignalerPage from './pages/SignalerPage'
import AgendaPage from './pages/AgendaPage'
import AdminCampagnesPage from './pages/AdminCampagnesPage'
import AdminEtablissementsPage from './pages/AdminEtablissementsPage'
import PrendreRdvPage from './pages/PrendreRdvPage'
import PaiementPage from './pages/PaiementPage'
import MesRendezVous from './pages/MesRendezVous'
import AgendaPro from './pages/AgendaPro'
import DashboardPage from './pages/DashboardPage'
import './App.css'

// Remet la page en haut à chaque changement de page
function RemonterEnHaut() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

// L'accueil garde son propre pied de page ; toutes les autres pages affichent celui-ci
function PiedDePage() {
  const { pathname } = useLocation()
  return pathname === '/' ? null : <Footer />
}

export default function App() {
  return (
    <div className="app">
      <RemonterEnHaut />
      <Navbar />
      <main className="contenu">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/annuaire" element={<CartePage />} />
          <Route path="/etablissements" element={<EtablissementsPage />} />
          <Route path="/sos" element={<SosPage />} />
          <Route path="/chaleur" element={<ChaleurPage />} />
          <Route path="/signaler" element={<SignalerPage />} />
          <Route path="/agenda" element={<AgendaPage />} />
          <Route path="/connexion" element={<ConnexionPage />} />
          <Route path="/inscription" element={<InscriptionPage />} />
          <Route
            path="/rdv/:id"
            element={
              <RouteProtegee roles={['PATIENT']}>
                <PrendreRdvPage />
              </RouteProtegee>
            }
          />
          <Route
            path="/paiement/:reference"
            element={
              <RouteProtegee roles={['PATIENT']}>
                <PaiementPage />
              </RouteProtegee>
            }
          />
          <Route
            path="/rendez-vous"
            element={
              <RouteProtegee roles={['PATIENT']}>
                <MesRendezVous />
              </RouteProtegee>
            }
          />
          <Route
            path="/pro"
            element={
              <RouteProtegee roles={['PROFESSIONNEL']}>
                <EspacePro />
              </RouteProtegee>
            }
          />
          <Route
            path="/pro/agenda"
            element={
              <RouteProtegee roles={['PROFESSIONNEL']}>
                <AgendaPro />
              </RouteProtegee>
            }
          />
          <Route
            path="/moderation"
            element={
              <RouteProtegee roles={['MODERATEUR', 'ADMIN_SANTE']}>
                <ModerationPage />
              </RouteProtegee>
            }
          />
          <Route
            path="/admin/campagnes"
            element={
              <RouteProtegee roles={['ADMIN_SANTE']}>
                <AdminCampagnesPage />
              </RouteProtegee>
            }
          />
          <Route
            path="/admin/tableau-de-bord"
            element={
              <RouteProtegee roles={['ADMIN_SANTE']}>
                <DashboardPage />
              </RouteProtegee>
            }
          />
          <Route
            path="/admin/etablissements"
            element={
              <RouteProtegee roles={['ADMIN_SANTE']}>
                <AdminEtablissementsPage />
              </RouteProtegee>
            }
          />
        </Routes>
      </main>
      <PiedDePage />
    </div>
  )
}