package udea.edu.co.googlesignindemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
    GoogleApiClient.OnConnectionFailedListener {

        private static final String TAG = MainActivity.class.getSimpleName();
        private static final int RC_SIGN_IN = 007;

        private GoogleApiClient mGoogleApiClient;
        private ProgressDialog mProgressDialog;

        private SignInButton btnSignIn;
        private Button btnSignOut;
        private LinearLayout llProfileLayout;
        private ImageView imgProfilePic;
        private TextView txtName, txtEmail;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
            btnSignOut = (Button) findViewById(R.id.btn_sign_out);
            llProfileLayout = (LinearLayout) findViewById(R.id.llProfile);
            imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
            txtName = (TextView) findViewById(R.id.txtName);
            txtEmail = (TextView) findViewById(R.id.txtEmail);

            btnSignIn.setOnClickListener(this);
            btnSignOut.setOnClickListener(this);

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            // Customizing G+ button
            btnSignIn.setSize(SignInButton.SIZE_STANDARD);
            btnSignIn.setScopes(gso.getScopeArray());
        }

    /**
     * Método para realizar inicio de sesión con cuenta de google plus
     */
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Método para desconectar usuario de la cuenta de google
     */
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        updateUI(false);
                    }
                });
    }


    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Se ha iniciado sesión correctamente, se muestra la UI autenticada.
            GoogleSignInAccount acct = result.getSignInAccount();

            //Log.e(TAG, "display name: " + acct.getDisplayName());

            String personName = acct.getDisplayName();
            String personPhotoUrl = "";
            if (acct.getPhotoUrl()==null){
                personPhotoUrl = "http://academialexiway.com/images/Inicio/opiniones/icono-chico.png";
                Log.d("Tag2", personPhotoUrl);
            }else{
                personPhotoUrl = acct.getPhotoUrl().toString();
            }
            String email = acct.getEmail();


            txtName.setText(personName);
            txtEmail.setText(email);

            Glide.with(getApplicationContext()).load(personPhotoUrl)
                    .thumbnail(0.5f)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imgProfilePic);

            updateUI(true);
        } else {
            // Se ha cerrado sesion, muestra una UI no autenticada.
            updateUI(false);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_sign_in:
                signIn();
                break;

            case R.id.btn_sign_out:
                signOut();
                break;

        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // Si las credenciales almacenadas en caché del usuario son válidas, el OptionalPendingResult será "done"
            // y el GoogleSignInResult estará disponible al instante.
            //Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();

            handleSignInResult(result);
        } else {
            // Si el usuario no se ha registrado previamente en el dispositivo o si el inicio de sesión ha caducado,
            // esta rama asíncrona intentará iniciar sesión en el usuario de forma silenciosa.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Se ha producido un error no resuelto y las API de Google (incluido el inicio de sesión) no estarán disponibles.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            btnSignIn.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.VISIBLE);
            llProfileLayout.setVisibility(View.VISIBLE);
        } else {
            btnSignIn.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.GONE);
            llProfileLayout.setVisibility(View.GONE);
        }
    }
}
