package gsoptimizer.Data;

import automata.fsa.FiniteStateAutomaton;
import file.XMLCodec;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JFLAPData extends Data {

    public JFLAPData() {
        super();
    }

    public JFLAPData(Data data) {
        super(data);
    }

    public JFLAPData(String filepath) {
        super();
        this.filepath = filepath;

        try {
            List<String> gsStrings = Files.readAllLines(Paths.get(filepath),
                    StandardCharsets.UTF_8);
            gsStrings.stream().forEach((s) -> {
                printData += s + "\n";
            });

            FiniteStateAutomaton automaton = (FiniteStateAutomaton) new XMLCodec()
                    .decode(new File(filepath), null);

            initialStateId = automaton.getInitialState().getID();

            HashSet<Integer> finalStates = new HashSet<>();

            for (automata.State s : automaton.getFinalStates()) {
                finalStates.add(s.getID());
            }

            for (automata.State s : automaton.getStates()) {
                State state = new State(s.getID(), s.getName())
                        .setLocation(s.getPoint().x, s.getPoint().y)
                        .setInitialState(s.getID() == initialStateId)
                        .setFinalState(finalStates.contains(s.getID()));
                if (s.getLabel() != null) {
                    state.setLabel(s.getLabel());
                }
                states.add(state);
                nameToId.put(s.getName(), s.getID());
                idToName.put(s.getID(), s.getName());
            }

            for (automata.Transition t : automaton.getTransitions()) {
                String message = t.getDescription();
                if (message.equals("λ")) {
                    message = "";
                }
                Transition transition = new Transition(t.getFromState().getID(), t.getToState().getID(), message);
                transitions.add(transition);
            }

        } catch (IOException ex) {
            Logger.getLogger(JFLAPData.class.getName()).log(Level.SEVERE, null, ex);
        }
        printData = "";
    }

    @Override
    public String toString() {
        if (printData == null || printData.equals("")) {
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = builder.newDocument();

                Element rootElement = document.createElement("structure");
                Element typeElement = document.createElement("type");
                typeElement.appendChild(document.createTextNode("fa"));
                Element automatonElement = document.createElement("automaton");

                for (State state : states) {
                    Element stateElement = document.createElement("state");
                    stateElement.setAttribute("id", String.valueOf(state.getId()));
                    stateElement.setAttribute("name", state.getName());

                    Element xElement = document.createElement("x");
                    xElement.appendChild(document.createTextNode(String.valueOf(state.getX())));
                    Element yElement = document.createElement("y");
                    yElement.appendChild(document.createTextNode(String.valueOf(state.getY())));

                    stateElement.appendChild(xElement);
                    stateElement.appendChild(yElement);

                    if (state.isInitialState()) {
                        stateElement.appendChild(document.createElement("initial"));
                    }
                    if (state.isFinalState()) {
                        stateElement.appendChild(document.createElement("final"));
                    }

                    automatonElement.appendChild(stateElement);
                }

                for (Transition transition : transitions) {
                    Element transitionElement = document.createElement("transition");
                    Element fromElement = document.createElement("from");
                    fromElement.appendChild(document.createTextNode(String.valueOf(transition.getFromState())));

                    Element toElement = document.createElement("to");
                    toElement.appendChild(document.createTextNode(String.valueOf(transition.getToState())));

                    Element readElement = document.createElement("read");
                    readElement.appendChild(document.createTextNode(transition.getMessage()));

                    transitionElement.appendChild(fromElement);
                    transitionElement.appendChild(toElement);
                    transitionElement.appendChild(readElement);
                    automatonElement.appendChild(transitionElement);
                }

                rootElement.appendChild(typeElement);
                rootElement.appendChild(automatonElement);
                document.appendChild(rootElement);

                DOMSource domsource = new DOMSource(document);
                StringWriter stringWriter = new StringWriter();
                StreamResult streamResult = new StreamResult(stringWriter);

                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domsource, streamResult);

                printData = stringWriter.toString();

            } catch (ParserConfigurationException ex) {
                Logger.getLogger(JFLAPData.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(JFLAPData.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerException ex) {
                Logger.getLogger(JFLAPData.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return printData;
    }
}
