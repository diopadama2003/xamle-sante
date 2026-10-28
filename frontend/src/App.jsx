import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import RouteProtegee from './components/RouteProtegee'
import CartePage from './pages/CartePage'
import ConnexionPage from './pages/ConnexionPage'
import InscriptionPage from './pages/InscriptionPage'
import EspacePro from './pages/EspacePro'
import ModerationPage from './pages/ModerationPage'
import SosPage from './pages/SosPage'
import ChaleurPage from './pages/ChaleurPage'
import SignalerPage from './pages/SignalerPage'
import AgendaPage from './pages/AgendaPage'
import AdminCampagnesPage from './pages/AdminCampagnesPage'
import PrendreRdvPage from './pages/PrendreRdvPage'
import PaiementPage from './pages/PaiementPage'
import MesRendezVous from './pages/MesRendezVous'
import AgendaPro from './pages/AgendaPro'
import DashboardPage from './pages/DashboardPage'
import './App.css'

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<CartePage />} />
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
      </Routes>
    </>
  )
}