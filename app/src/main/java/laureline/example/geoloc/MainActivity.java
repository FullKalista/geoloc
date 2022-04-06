package laureline.example.geoloc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

// étape 1 : initialisations
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.View.OnClickListener;
import android.view.View;

// étape 2 : choix de la source
import android.app.AlertDialog.Builder;
import java.util.List;
import android.location.LocationManager;
import android.content.DialogInterface;
import android.content.Context;

// étape 3 : obtenir la position
import android.location.LocationListener;
import android.location.Location;
import android.widget.Toast;

// étape 4 : obtenir l'adresse
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // déclarations étape 1 : initialisations
    private Button buttonChoisirSource;         // bouton pour choisir la source
    private Button buttonObtenirPosition;         // bouton pour obtenir la position
    private Button buttonAfficherAdresse;       // bouton pour obtenir l'adresse
    private TextView textViewSource;            // pour afficher la source choisie
    private TextView textViewLatitude;          // pour afficher la latitude
    private TextView textViewLongitude;         // pour afficher la longitude
    private TextView textViewAltitude;          // pour afficher l'altitude
    private TextView textViewAdresse;           // pour afficher l'adresse
    private ProgressBar progressBar;            // pour afficher le cercle de chargement

    // déclarations étape 2 : choix de la source
    private LocationManager serviceLocalisation;  // le service de géolocalisation
    private String[] tableauSources;              // pour mémoriser les sources de géolocalisation
    private String sourceChoisie = "";            // nom de la source choisie ("gps", "network", "passive")

    // déclarations étape 3 : obtenir la position
    private Location positionGeographique;      // la position géographique

    // pour récupérer les libellés à partir de la ressource string.xml
    private String nomApplication, libelleSource, libelleLatitude;
    private String libelleLongitude, libelleAltitude, libelleAdresse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialisations();
    }

    /** Fonction appelée par l'évènement onCreate. */
    public void initialisations(){
        // on récupère tous les éléments de notre interface graphique grâce aux ID
        buttonChoisirSource = (Button) findViewById(R.id.buttonChoisirSource);
        buttonObtenirPosition = (Button) findViewById(R.id.buttonObtenirPosition);
        buttonAfficherAdresse = (Button) findViewById(R.id.buttonAfficherAdresse);
        textViewSource = (TextView) findViewById(R.id.textViewSource);
        textViewLatitude = (TextView) findViewById(R.id.textViewLatitude);
        textViewLongitude = (TextView) findViewById(R.id.textViewLongitude);
        textViewAltitude = (TextView) findViewById(R.id.textViewAltitude);
        textViewAdresse = (TextView) findViewById(R.id.textViewAdresse);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // récupération des libellés à partir de la ressource string.xml
        nomApplication = getString(R.string.app_name);
        libelleSource = getString(R.string.txtSourceChoisie);
        libelleLatitude = getString(R.string.txtLatitude);
        libelleLongitude = getString(R.string.txtLongitude);
        libelleAltitude = getString(R.string.txtAltitude);
        libelleAdresse = getString(R.string.txtAdresse);

        // initialisation de l'écran
        reinitialiserEcran();

        // affectation d'un écouteur d'évènement aux boutons à l'aide de classes internes
        buttonChoisirSource.setOnClickListener(new buttonChoisirSourceClickListener());
        buttonObtenirPosition.setOnClickListener(new buttonObtenirPositionClickListener());
        buttonAfficherAdresse.setOnClickListener(new buttonAfficherAdresseClickListener());

        // récupération du service de localisation du mobile
        serviceLocalisation = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    /** réinitialisation de l'écran */
    private void reinitialiserEcran(){
        // affichage de valeurs par défaut
        textViewSource.setText(libelleSource);
        textViewLatitude.setText(libelleLatitude);
        textViewLongitude.setText(libelleLongitude);
        textViewAltitude.setText(libelleAltitude);
        textViewAdresse.setText(libelleAdresse);

        // désactivation de 2 boutons et du ProgressBar
        buttonObtenirPosition.setEnabled(false);
        buttonAfficherAdresse.setEnabled(false);
        progressBar.setVisibility(View.GONE);
    }

    /** Gestion du choix de la source */
    private class buttonChoisirSourceClickListener implements OnClickListener{
        public void onClick(View v){
            // réinitialisation de l'écran
            reinitialiserEcran();

            // demander la liste des sources disponibles au service de géolocalisation
            List<String> listeSources = serviceLocalisation.getProviders(true);

            // transférer les sources depuis la liste vers un tableau de String
            tableauSources = new String[listeSources.size()];
            int i = 0;
            for (String uneSource : listeSources){
                tableauSources[i++] = uneSource;
            }

            // crétaion d'une boîte de dialogue permettant à l'utilisateur de choisir une source
            Builder laBoiteDeDialogue = new Builder(MainActivity.this);

            // ajoute la liste des sources et un écouteur d'évènement à la boîte de dialogue
            laBoiteDeDialogue.setItems(tableauSources, new DialogueClickListener());

            // affiche la boîte de dialogue
            laBoiteDeDialogue.create().show();
        }
    }

    /** classe interne pour gérer le choix dans la boîte de dialogue */
    private class DialogueClickListener implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface unDialogue, int indexChoisi) {
            // active le bouton permettant d'obtenir la position
            buttonObtenirPosition.setEnabled(true);

            // mémorise la source choisie
            sourceChoisie = tableauSources[indexChoisi];

            // ajoute dans la barre de titre de l'application le nom de la source choisie
            setTitle(nomApplication + " - " + sourceChoisie);

            // affiche la source choisie
            textViewSource.setText(libelleSource + " " + sourceChoisie);
        }
    }

    /** Gestion de la géolocalisation */
    private class buttonObtenirPositionClickListener implements OnClickListener{
        public void onClick(View v){
            // démarre le cercle de chargement
            progressBar.setVisibility(View.VISIBLE);

            // demande au service de localisation de modifier tout changement de position :
            //      sur la source choisie (le provider)
            //      toutes les minutes (60 000 millisecondes)
            //      quelle que soit la distance entre 2 mesures (0)
            try{
                serviceLocalisation.requestLocationUpdates(sourceChoisie,
                        60000, 0, new monLocationListener());
            }
            catch (SecurityException ex){
                String msg = "La géolocalisation est impossible !";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Gestion de l'obtention de l'adresse */
    private class buttonAfficherAdresseClickListener implements OnClickListener{
        public void onClick(View v){
            // démarre le cercle de chargement
            progressBar.setVisibility(View.VISIBLE);

            // le gocoder permet de récupérer ou chercher des adresses grâce à un mot clé ou une position
            Geocoder unGeocder = new Geocoder(MainActivity.this);
            try{
                // ici on récupère la première adresse trouvée grâce à la position que l'on a récupérée
                List<Address> listeAdresses = unGeocder.getFromLocation(positionGeographique.getLatitude(), positionGeographique.getLongitude(), 1);
                if (listeAdresses != null && listeAdresses.size() == 1){
                    Address adresseTrouvee = listeAdresses.get(0);

                    // si le geocoder a trouvé une adresse, alors on l'affiche
                    String rue = adresseTrouvee.getAddressLine(0);
                    String cp = adresseTrouvee.getPostalCode();
                    String ville = adresseTrouvee.getLocality();
                    String msg = rue + "\n" + cp + "\n" + ville;
                    textViewAdresse.setText(libelleAdresse + "\n\n" + msg);
                }
                else{
                    // sinon on affiche un message d'erreur
                    textViewAdresse.setText(libelleAdresse + "\n" + "L'adresse n'a pas pu être déterminée");
                }
            }
            catch (IOException e){
                e.printStackTrace();
                textViewAdresse.setText(libelleAdresse + "\n" + e.getMessage());
            }
            // arrête le cercle de chargement
            progressBar.setVisibility(View.GONE);
        }
    }

    /** classe interne pour gérer la géolocalisation */
    private class monLocationListener implements LocationListener{
        public void onLocationChanged(Location unePosition){
            // lorsque la position change, on affiche un Toast pour le signaler à l'utilisateur
            String msg = "La position a changé";
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

            // on arrête le cercle de chargement
            progressBar.setVisibility(View.GONE);

            // on active le bouton pour afficher l'adresse
            buttonAfficherAdresse.setEnabled(true);

            // on sauvegarde la position
            positionGeographique = unePosition;

            // et on l'affiche
            afficherPosition();

            // et on spécifie au service que l'on ne souhaite plus avoir de mise à jour
            try{
                serviceLocalisation.removeUpdates((LocationListener) this);
            }
            catch (SecurityException ex){

            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras){
            // lorsque l'état de la source change, on affiche un Toast pour le signaler à l'utilisateur
            String msg = "La source " + provider + "a changé d'état";
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider){
            // lorsque la source est réactivée, on affiche un Toast pour le signaler à l'utilisateur
            String msg = "La source" + provider + "a été réactivée";
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }

        public void onProviderDisabled(String provider){
            // lorsque la source est réactivée, on affiche un Toast pour le signaler à l'utilisateur
            String msg = "La source" + provider + "a été désactivée";
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

            // on arrête le cercle de chargement
            setProgressBarIndeterminateVisibility(false);

            // et on spécifie au service que l'on ne souhaite plus avoir de mise à jour
            try{
                serviceLocalisation.removeUpdates((LocationListener) this);
            }
            catch (SecurityException ex){

            }
        }

        /** affichage de la position géographique */
        private void afficherPosition(){
            // affiche les informations de la position à l'écran (on limite la longueur affichée à 10 caractères)
            String msg = String.valueOf(positionGeographique.getLatitude());
            if (msg.length() > 10) msg = msg.substring(0, 10);
            textViewLatitude.setText(libelleLatitude + " " + msg);

            msg = String.valueOf(positionGeographique.getLongitude());
            if (msg.length() > 10) msg = msg.substring(0, 10);
            textViewLongitude.setText(libelleLongitude + " " + msg);

            msg = String.valueOf(positionGeographique.getAltitude());
            textViewAltitude.setText(libelleAltitude + " " + msg);
        }
    }
}