/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import Models.Player;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import Models.partie.Action;
import Models.partie.Grille;
import Models.partie.Partie;
import java.io.File;
import javax.swing.JOptionPane;

/**
 *
 * @author p1704450
 */
public class Connexion {
    
    private static Connection connection = null;
    
    public Connexion(){
        this.connect();
    }
        
    private void connect(){
        String fileURL = "jdbc:sqlite:src/database/dbMorpion.sqlite";
        try {
            connection = DriverManager.getConnection(fileURL);
        } catch (SQLException ex) {
            System.out.println("Erreur de connexion a la base de donnees!");
        }
    }
    
    public void supprimerPartie(int idPartie){
        try {
            String query= "DELETE FROM 'Partie' WHERE idPartie = " + idPartie;
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(query);
            query= "DELETE FROM 'Case' WHERE idPartie = " + idPartie;
            stmt.executeUpdate(query);
            query= "DELETE FROM 'Replay' WHERE idPartie = " + idPartie;
            stmt.executeUpdate(query);
        } catch (SQLException ex) {
            Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Partie chargerPartie(int idPartie){
        try {
            //Données Partie
            String queryPartie = "SELECT * FROM 'Partie' WHERE idPartie = " + idPartie;
            Statement stmtPartie = connection.createStatement();
            ResultSet rsPartie = stmtPartie.executeQuery(queryPartie);
            String j1 = rsPartie.getString(2);
            String j2 = rsPartie.getString(3);
            int scoreJ1 = rsPartie.getInt(4);
            int scoreJ2 = rsPartie.getInt(5);
            rsPartie.close();
            Partie partie = new Partie(idPartie, j1, j2, scoreJ1, scoreJ2);
            
            //Données Cases
            String queryCases = "SELECT * FROM 'Case' WHERE idPartie = " + idPartie;
            Statement stmtCases = connection.createStatement();
            ResultSet rsCases = stmtCases.executeQuery(queryCases);
            while(rsCases.next()){
                partie.getGrille().setCase(rsCases.getInt(3), rsCases.getInt(2), rsCases.getInt(4));
            }
            rsCases.close();
            
            //Données Actions
            String queryActions = "SELECT * FROM 'Replay' WHERE idPartie = " + idPartie+" ORDER BY numCoup";
            Statement stmtActions = connection.createStatement();
            ResultSet rsActions = stmtActions.executeQuery(queryActions);
            while(rsActions.next()){
                partie.getGrille().ajouterAction(new Action(rsActions.getInt(4), rsActions.getInt(3), rsActions.getInt(5)));
            }
            rsActions.close();
            return partie;
        } catch (SQLException ex) {
            System.out.println("Erreur chargement");
        }
            return null;
    }
    
    public boolean sauvegarderPartie(Partie partie){
        int idPartie = partie.getIdPartie();
        boolean success = false;
        try {
            if(gameIsInDatabase(idPartie)){
                //game exists in DB
                success =  mettreAJourPartie(partie);                
                return success;
            }else{
                //game doesn't exist in DB
                success =  ajouterNouvellePartie(partie);
                return success;
            }
                
            } catch (SQLException ex) {
                Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Erreur lors de la sauvegarde de la partie.");
            }
        return false;
    }

    private boolean mettreAJourPartie(Partie partie){
        int idPartie = partie.getIdPartie();
        try {
            //modif table Partie
            String queryPartie =  "UPDATE 'Partie'"
                            + " SET scoreJoueur1 = " + partie.getScoreJoueur1()+","
                            + " scoreJoueur2 = " + partie.getScoreJoueur2()+","
                            + " time = CURRENT_TIMESTAMP"
                            + " WHERE idPartie = " + idPartie;
            Statement stmtPartie = connection.createStatement();
            stmtPartie.executeUpdate(queryPartie);
            stmtPartie.close();

            //modif table Case
            for(int y = 0; y < 3; y++){
                for(int x = 0; x < 3; x++){
                        String queryCase =  "UPDATE 'Case'"
                                + " SET contenu = " + partie.getGrille().getCase(x, y).getContenu()
                                + " WHERE idPartie = " + idPartie + " AND posX = " + x + " AND posY = " + y;
                        Statement stmtCase = connection.createStatement();
                        stmtCase.executeUpdate(queryCase);
                        stmtCase.close();
                }
            }
            
            ArrayList<Action> liste = partie.getGrille().getListeAction();
            supprimerReplay(idPartie);
            for(int i = 0 ; i < liste.size() ; i++){
                Action a = liste.get(i);
                String queryCoup = "insert into Replay values ("
                        +i+","
                        +idPartie+","
                        +a.getPosX()+","
                        +a.getPosY()+","
                        +a.getValeur()+
                        ");";
                Statement stmtCoup = connection.createStatement();
                stmtCoup.executeUpdate(queryCoup);
                stmtCoup.close();
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
    
    private void supprimerReplay(int idPartie) throws SQLException{
            Statement stmt = connection.createStatement();
            String query= "DELETE FROM 'Replay' WHERE idPartie = " + idPartie;
            stmt.executeUpdate(query);
    }
    
    public void nouvelleManche(Partie partie){
        try {
            int idPartie = partie.getIdPartie();
            supprimerReplay(idPartie);
            Statement stmt = connection.createStatement();
            String queryCases = "UPDATE 'Case' SET contenu = 0 WHERE idPartie = "+idPartie;
            stmt.executeUpdate(queryCases);
            String queryPartie = "UPDATE 'Partie' SET "
                    + "scoreJoueur1 = "+partie.getScoreJoueur1()+","
                    + "scoreJoueur2 = "+partie.getScoreJoueur2()+" "
                    + "WHERE idPartie = "+idPartie;
            stmt.executeUpdate(queryPartie);
        } catch (SQLException ex) {
            Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    private boolean ajouterNouvellePartie(Partie partie) {
        try {
            //modif table Partie
            String queryPartie =  "INSERT INTO 'Partie'(nomJoueur1, nomJoueur2, scoreJoueur1, scoreJoueur2) VALUES"
                            + "(?,?,?,?)";
            PreparedStatement stmt = connection.prepareStatement(queryPartie);
            stmt.setString(1, partie.getJoueur1());
            stmt.setString(2, partie.getJoueur2());
            stmt.setInt(3, 0);
            stmt.setInt(4, 0);
            stmt.executeUpdate();
            
            int idPartie = getLastGameId();
            
            partie.setIdPartie(idPartie);
            
            Statement stmtCases = connection.createStatement();
            //modif table Case
            for(int y = 0; y < 3; y++){
                for(int x = 0; x < 3; x++){
                        String queryCase =  "INSERT INTO 'Case' VALUES"
                                + " ( " + idPartie +", " + y +", " + x + ", " + partie.getGrille().getCase(y, x).getContenu()+" ) ";
                        stmtCases.executeUpdate(queryCase);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private boolean gameIsInDatabase(int idPartie) throws SQLException{
        ResultSet rs = null;
        String query = "SELECT idPartie FROM 'Partie' WHERE idPartie = " + idPartie;
        Statement stmt = connection.createStatement();
        rs = stmt.executeQuery(query);
        if(rs.next()){
            rs.close();
            return true;
        }else{
            rs.close();
            return false;
        }
    }
    
    private int getLastGameId() throws SQLException{
        String query = "SELECT idPartie FROM 'Partie' ORDER BY idPartie desc LIMIT 1";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        if(rs.next()){
            int id = rs.getInt(1);
            rs.close();
            return id;
        }
        rs.close();
        return -1;
    }
    
    public HashMap<Integer,String> getListeParties(){
        try {
            HashMap<Integer,String>listePartie = new HashMap();
            String query = "SELECT idPartie, nomJoueur1, nomJoueur2, time from `Partie`";
            Statement stmt = connection.createStatement();
            ResultSet rsPartie = stmt.executeQuery(query);
            while(rsPartie.next()){
                listePartie.put(rsPartie.getInt(1),rsPartie.getString(2)+" "+rsPartie.getString(3)+" - "+rsPartie.getString(4));
            }
            rsPartie.close();
            stmt.close();
            return listePartie;
        } catch (SQLException ex) {
            Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ArrayList<Player> loadJoueurs() {
        try {
            ArrayList<Player> listeJoueurs = new ArrayList();
            String query = "SELECT nomJoueur1, nomJoueur2, scoreJoueur1, scoreJoueur2 from `Partie`";
            Statement stmt = connection.createStatement();
            ResultSet rsPartie = stmt.executeQuery(query);
            while(rsPartie.next()){
                String nomJ1 = rsPartie.getString(1);
                String nomJ2 = rsPartie.getString(2);
                int scoreJ1 = rsPartie.getInt(3);
                int scoreJ2 = rsPartie.getInt(4);
                Player p1 = listePlayerContainsPlayer(listeJoueurs, nomJ1);
                if(!nomJ1.equals("")){
                    if(p1 != null){
                        p1.setNbParties(p1.getNbParties()+1);
                        p1.setNbManchesTot(p1.getNbManchesTot()+scoreJ1+scoreJ2);
                        p1.setNbManchesGag(p1.getNbManchesGag()+scoreJ1);
                    }else{
                        p1 = new Player(nomJ1);
                        p1.setNbParties(1);
                        p1.setNbManchesTot(scoreJ1+scoreJ2);
                        p1.setNbManchesGag(scoreJ1);
                        listeJoueurs.add(p1);
                    }
                }
                Player p2 = listePlayerContainsPlayer(listeJoueurs, nomJ2);
                if(p2 != null){
                    p2.setNbParties(p2.getNbParties()+1);
                    p2.setNbManchesTot(p2.getNbManchesTot()+scoreJ2+scoreJ1);
                    p2.setNbManchesGag(p2.getNbManchesGag()+scoreJ2);
                }else{
                    p2 = new Player(nomJ2);
                    p2.setNbParties(1);
                    p2.setNbManchesTot(scoreJ2+scoreJ1);
                    p2.setNbManchesGag(scoreJ2);
                    listeJoueurs.add(p2);
                }
            }
            rsPartie.close();
            stmt.close();
            return listeJoueurs;
        } catch (SQLException ex) {
            Logger.getLogger(Connexion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private Player listePlayerContainsPlayer(ArrayList<Player> listePlayer, String playerName){
        for(Player p : listePlayer){
            if(p.getName().equals(playerName)){
                return p;
            }
        }
        return null;
    }
}
