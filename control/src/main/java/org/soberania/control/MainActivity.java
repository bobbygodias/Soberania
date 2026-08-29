package org.soberania.control;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int SHIZUKU_REQUEST = 4701;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<AppEntry> apps = new ArrayList<>();

    private TextView status;
    private TextView output;
    private Spinner appA;
    private Spinner appB;
    private EditText manualCommand;

    private IControlService controlService;

    private final Shizuku.OnBinderReceivedListener binderReceived = () -> {
        renderStatus("Shizuku detectado. Verificando autorização...");
        ensureShizukuPermission();
    };

    private final Shizuku.OnBinderDeadListener binderDead = () -> {
        controlService = null;
        renderStatus("Shizuku desconectado.");
    };

    private final Shizuku.OnRequestPermissionResultListener permissionResult = (requestCode, grantResult) -> {
        if (requestCode != SHIZUKU_REQUEST) return;
        if (grantResult == PERMISSION_GRANTED) {
            bindShellService();
        } else {
            renderStatus("Permissão do Shizuku negada.");
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            controlService = IControlService.Stub.asInterface(binder);
            try {
                int remoteUid = controlService.uid();
                renderStatus("Pronto. Serviço remoto UID " + remoteUid +
                        (remoteUid == 2000 ? " (shell/ADB)." : remoteUid == 0 ? " (root)." : "."));
            } catch (RemoteException e) {
                renderStatus("Serviço conectado, mas UID indisponível: " + e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            controlService = null;
            renderStatus("Serviço shell desconectado.");
        }
    };

    private final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(
                    new ComponentName(BuildConfig.APPLICATION_ID, ShellUserService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("shell")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        loadLaunchableApps();

        Shizuku.addBinderReceivedListenerSticky(binderReceived);
        Shizuku.addBinderDeadListener(binderDead);
        Shizuku.addRequestPermissionResultListener(permissionResult);

        if (!Shizuku.pingBinder()) {
            renderStatus("Shizuku ainda não está disponível. Inicie-o e volte para cá.");
        }
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived);
        Shizuku.removeBinderDeadListener(binderDead);
        Shizuku.removeRequestPermissionResultListener(permissionResult);
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(14);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("SOBERANIA CONTROLE");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Sem root. Shizuku. Sem telemetria. O aparelho é seu.");
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        status = new TextView(this);
        status.setText("Inicializando...");
        status.setTextIsSelectable(true);
        root.addView(status);

        root.addView(button("Autorizar / reconectar Shizuku", v -> ensureShizukuPermission()));
        root.addView(button("Abrir Opções do desenvolvedor", v -> openDeveloperSettings()));
        root.addView(button("Diagnóstico do aparelho", v -> runShell(diagnosticCommand())));

        addHeader(root, "Aplicativo A");
        appA = new Spinner(this);
        root.addView(appA);

        addHeader(root, "Aplicativo B");
        appB = new Spinner(this);
        root.addView(appB);

        root.addView(button("A: FORÇAR REDIMENSIONAMENTO", v ->
                withA(a -> runShell("am compat enable FORCE_RESIZE_APP " + sq(a.packageName)))));

        root.addView(button("A: RESTAURAR POLÍTICA DE JANELA", v ->
                withA(a -> runShell(
                        "am compat disable FORCE_RESIZE_APP " + sq(a.packageName) +
                        "; am compat disable FORCE_NON_RESIZE_APP " + sq(a.packageName)))));

        root.addView(button("DIVIDIR A | B", v -> splitSelectedApps()));

        root.addView(button("A: ABRIR EM TELA CHEIA", v ->
                withA(a -> runShell("am start -W --windowingMode 1 -n " + sq(a.component.flattenToString())))));

        addHeader(root, "Compatibilidade global");
        root.addView(button("Forçar apps redimensionáveis: ON", v ->
                runShell("settings put global force_resizable_activities 1; " +
                        "settings get global force_resizable_activities")));

        root.addView(button("Forçar apps redimensionáveis: PADRÃO", v ->
                runShell("settings delete global force_resizable_activities; " +
                        "settings get global force_resizable_activities")));

        addHeader(root, "Segundo plano — Aplicativo A");
        root.addView(button("A: LIBERAR SEGUNDO PLANO", v ->
                withA(a -> runShell(
                        "cmd appops set " + sq(a.packageName) + " RUN_IN_BACKGROUND allow; " +
                        "cmd appops set " + sq(a.packageName) + " RUN_ANY_IN_BACKGROUND allow; " +
                        "am set-standby-bucket " + sq(a.packageName) + " active; " +
                        "am get-standby-bucket " + sq(a.packageName)))));

        root.addView(button("A: RESTRINGIR SEGUNDO PLANO", v ->
                withA(a -> runShell(
                        "cmd appops set " + sq(a.packageName) + " RUN_IN_BACKGROUND ignore; " +
                        "cmd appops set " + sq(a.packageName) + " RUN_ANY_IN_BACKGROUND ignore; " +
                        "am set-standby-bucket " + sq(a.packageName) + " restricted; " +
                        "am get-standby-bucket " + sq(a.packageName)))));

        addHeader(root, "DPM / diagnóstico");
        root.addView(button("Mostrar proprietários/administradores", v ->
                runShell("dpm list-owners 2>&1 || dpm list active-admins 2>&1 || dumpsys device_policy | head -n 120")));

        addHeader(root, "Shell avançado (UID do Shizuku)");
        manualCommand = new EditText(this);
        manualCommand.setSingleLine(false);
        manualCommand.setMinLines(2);
        manualCommand.setHint("Ex.: settings get global force_resizable_activities");
        root.addView(manualCommand);
        root.addView(button("EXECUTAR COMANDO", v -> runManualCommand()));

        addHeader(root, "Saída");
        output = new TextView(this);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setMinLines(8);
        output.setPadding(0, dp(6), 0, dp(32));
        root.addView(output);

        return scroll;
    }

    private void addHeader(LinearLayout root, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextSize(17);
        v.setPadding(0, dp(18), 0, dp(6));
        root.addView(v);
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        return b;
    }

    @SuppressWarnings("deprecation")
    private void loadLaunchableApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(0);
        apps.clear();

        for (ApplicationInfo info : installed) {
            Intent launch = pm.getLaunchIntentForPackage(info.packageName);
            if (launch == null || launch.getComponent() == null) continue;
            CharSequence label = pm.getApplicationLabel(info);
            apps.add(new AppEntry(
                    label == null ? info.packageName : label.toString(),
                    info.packageName,
                    launch.getComponent()));
        }

        apps.sort(Comparator
                .comparing((AppEntry e) -> e.label, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.packageName));

        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, apps);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appA.setAdapter(adapter);

        ArrayAdapter<AppEntry> adapterB = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(apps));
        adapterB.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appB.setAdapter(adapterB);
    }

    private void ensureShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            renderStatus("Shizuku não está rodando.");
            return;
        }

        try {
            if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
                bindShellService();
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                renderStatus("Shizuku está rodando, mas a autorização foi negada. Libere no gerenciador Shizuku.");
            } else {
                renderStatus("Solicitando autorização ao Shizuku...");
                Shizuku.requestPermission(SHIZUKU_REQUEST);
            }
        } catch (Throwable t) {
            renderStatus("Falha ao falar com Shizuku: " + t.getMessage());
        }
    }

    private void bindShellService() {
        try {
            renderStatus("Autorizado. Iniciando serviço shell...");
            Shizuku.bindUserService(userServiceArgs, serviceConnection);
        } catch (Throwable t) {
            renderStatus("Não foi possível iniciar o serviço shell: " + t);
        }
    }

    private void splitSelectedApps() {
        AppEntry a = selected(appA);
        AppEntry b = selected(appB);
        if (a == null || b == null) return;
        if (a.packageName.equals(b.packageName)) {
            toast("Escolha dois aplicativos diferentes.");
            return;
        }

        String command =
                "echo '[1/6] compat A'; " +
                "am compat enable FORCE_RESIZE_APP " + sq(a.packageName) + "; " +
                "echo '[2/6] compat B'; " +
                "am compat enable FORCE_RESIZE_APP " + sq(b.packageName) + "; " +
                "echo '[3/6] suporte do aparelho'; " +
                "am supports-split-screen-multi-window 2>&1; " +
                "echo '[4/6] abrindo A como primário'; " +
                "am start -W --windowingMode 3 -n " + sq(a.component.flattenToString()) + " 2>&1; " +
                "sleep 1; " +
                "echo '[5/6] abrindo B como secundário'; " +
                "am start -W --windowingMode 4 -n " + sq(b.component.flattenToString()) + " 2>&1; " +
                "echo '[6/6] estado resumido'; " +
                "dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity|windowingMode' | head -n 40";

        runShell(command);
    }

    private String diagnosticCommand() {
        return "echo '=== SOBERANIA CONTROLE ==='; " +
                "echo SDK=$(getprop ro.build.version.sdk); " +
                "echo RELEASE=$(getprop ro.build.version.release); " +
                "echo FABRICANTE=$(getprop ro.product.manufacturer); " +
                "echo MODELO=$(getprop ro.product.model); " +
                "echo '--- split-screen ---'; " +
                "am supports-split-screen-multi-window 2>&1; " +
                "echo '--- force_resizable_activities ---'; " +
                "settings get global force_resizable_activities; " +
                "echo '--- enable_freeform_support ---'; " +
                "settings get global enable_freeform_support; " +
                "echo '--- DPM owners ---'; " +
                "(dpm list-owners 2>&1 || true)";
    }

    private void runManualCommand() {
        String cmd = manualCommand.getText().toString().trim();
        if (cmd.isEmpty()) {
            toast("Digite um comando.");
            return;
        }
        runShell(cmd);
    }

    private void runShell(String command) {
        IControlService service = controlService;
        if (service == null) {
            renderStatus("Serviço shell não está pronto. Tentando reconectar...");
            ensureShizukuPermission();
            return;
        }

        output.setText("$ " + command + "\n\nexecutando...");
        executor.execute(() -> {
            String result;
            try {
                result = service.exec(command);
            } catch (Throwable t) {
                result = t.getClass().getSimpleName() + ": " + t.getMessage();
                controlService = null;
            }
            String finalResult = result;
            runOnUiThread(() -> output.setText("$ " + command + "\n\n" + finalResult));
        });
    }

    private void openDeveloperSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Throwable t) {
            toast("Não consegui abrir as opções do desenvolvedor.");
        }
    }

    private void withA(AppConsumer consumer) {
        AppEntry a = selected(appA);
        if (a != null) consumer.accept(a);
    }

    private AppEntry selected(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        if (!(item instanceof AppEntry)) {
            toast("Nenhum aplicativo selecionado.");
            return null;
        }
        return (AppEntry) item;
    }

    private void renderStatus(String text) {
        runOnUiThread(() -> {
            status.setText(text);
            if (output != null && output.getText().length() == 0) {
                output.setText(text);
            }
        });
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String sq(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private interface AppConsumer {
        void accept(AppEntry app);
    }

    private static final class AppEntry {
        final String label;
        final String packageName;
        final ComponentName component;

        AppEntry(String label, String packageName, ComponentName component) {
            this.label = label;
            this.packageName = packageName;
            this.component = component;
        }

        @Override
        public String toString() {
            return label + " — " + packageName;
        }
    }
}
