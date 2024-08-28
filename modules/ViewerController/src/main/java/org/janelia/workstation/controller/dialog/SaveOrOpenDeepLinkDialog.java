package org.janelia.workstation.controller.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.action.OpenDeepLinkAction;
import org.janelia.workstation.controller.model.DeepLink;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SaveOrOpenDeepLinkDialog extends ModalDialog {
    private JTextArea deepLinkResults = new JTextArea(2,40);
    private JTextArea loadDeepLink = new JTextArea(2,40);
    private boolean success = false;

    public SaveOrOpenDeepLinkDialog() {
        super(FrameworkAccess.getMainFrame());

        setTitle("Save or Open Deep Link");
        setLayout(new GridBagLayout());

        // Enable line wrapping
        deepLinkResults.setLineWrap(true);
        deepLinkResults.setWrapStyleWord(true);
        deepLinkResults.setEditable(true);
        loadDeepLink.setLineWrap(true);
        loadDeepLink.setWrapStyleWord(true);
        loadDeepLink.setEditable(true);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 10, 0, 10);

        constraints.gridx = 0;
        constraints.gridy = 0;
        add(new JLabel("Generate DeepLink For Current Location:"), constraints);

        JButton generateButton = new JButton("Generate");
        generateButton.setToolTipText("Generate Deep Link");
        generateButton.addActionListener(e -> generateLink());

        constraints.gridx = 1;
        constraints.gridy = 0;
        add(generateButton,constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 5, 30, 5);
        add(deepLinkResults,constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        add(new JLabel("Load DeepLink:"), constraints);

        constraints.gridx = 1;
        constraints.gridy = 2;
        add(loadDeepLink,constraints);

        constraints.gridx = 2;
        constraints.gridy = 2;
        JButton loadButton = new JButton("Load");
        loadButton.setToolTipText("Load Deep Link");
        loadButton.addActionListener(e -> loadLink());
        add(loadButton, constraints);

        JButton closeButton = new JButton("Close");
        closeButton.setToolTipText("Close");
        closeButton.addActionListener(e -> onCancel());
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        add(closeButton, constraints);

        getRootPane().setDefaultButton(closeButton);
    }

    public void showDialog() {
        packAndShow();
    }

    private String encodeValue(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    private void onCancel() {
        success = false;
        dispose();
    }

    private void generateLink() {
        DeepLink deepLink = new DeepLink();

        Map<String, Object> deepLinkMap = new HashMap<>();
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        if (workspace==null) {
            TmSample sample = TmModelManager.getInstance().getCurrentSample();
            if (sample == null) {
                deepLinkResults.setText("There is no workspace or sample currently open to generate a deep link");
                return;
            }
            deepLink.setSample(sample);
            deepLinkMap.put("sample", sample.getId());
        } else {
            deepLink.setWorkspace(workspace);
            deepLinkMap.put("workspace", workspace.getId());
        }

        TmViewState view = TmModelManager.getInstance().getCurrentView();
        float[] vtxLocation = TmModelManager.getInstance().getLocationInMicrometers(view.getCameraFocusX(),
                view.getCameraFocusY(), view.getCameraFocusZ());
        deepLink.setViewpoint(view);
        deepLinkMap.put("viewFocusX", vtxLocation[0]);
        deepLinkMap.put("viewFocusY", vtxLocation[1]);
        deepLinkMap.put("viewFocusZ", vtxLocation[2]);
        deepLinkMap.put("viewZoom", view.getZoomLevel());

        ObjectMapper mapper = new ObjectMapper();
        String encodedURL = deepLinkMap.keySet().stream()
                .map(key -> {
                    try {
                        String jsonVal = mapper.writeValueAsString(deepLinkMap.get(key));
                        return key + "=" + encodeValue(jsonVal);
                    } catch (UnsupportedEncodingException | JsonProcessingException e) {
                        e.printStackTrace();
                        return "";
                    }
                })
                .collect(Collectors.joining("&", "deeplink:", ""));
        deepLinkResults.setText(encodedURL);
        repaint();
        revalidate();
    }

    private void loadLink() {
        String encodedParams = loadDeepLink.getText().replaceAll("deeplink:","");
        String[] pairs = encodedParams.split("&");

        HashMap<String,String> params = new HashMap<>();
        try {
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = null;
                key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.toString());
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.toString());
                params.put(key, value);
            }

            DeepLink parsedDeepLink = new DeepLink();
            ObjectMapper mapper = new ObjectMapper();
            if (params.containsKey("workspace")) {
                TmWorkspace workspace = TiledMicroscopeDomainMgr.getDomainMgr().getWorkspace(Long.parseLong(params.get("workspace")));
                parsedDeepLink.setWorkspace(workspace);
            } else if (params.containsKey("sample")) {
                TmSample sample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(Long.parseLong(params.get("sample")));
                parsedDeepLink.setSample(sample);
            }

            Double focusX = mapper.readValue(params.get("viewFocusX"), Double.class);
            Double focusY = mapper.readValue(params.get("viewFocusY"), Double.class);
            Double focusZ = mapper.readValue(params.get("viewFocusZ"), Double.class);
            Double zoomLevel = mapper.readValue(params.get("viewZoom"), Double.class);
            TmViewState view = new TmViewState();
            view.setCameraFocusX(focusX);
            view.setCameraFocusY(focusY);
            view.setCameraFocusZ(focusZ);
            view.setZoomLevel(zoomLevel);
            parsedDeepLink.setViewpoint(view);

            OpenDeepLinkAction action = new OpenDeepLinkAction();
            action.setDeepLink(parsedDeepLink);
            action.performAction();
            onCancel();
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }


    public boolean isSuccess() {
        return success;
    }

}
