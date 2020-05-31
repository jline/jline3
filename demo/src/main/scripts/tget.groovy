// END_HELP
import org.jline.utils.*
import org.jline.builtins.Options

class TerminalCapability {
    def terminal
    
    public TerminalCapability(def terminal) 
    {
        this.terminal = terminal
    }
    
    def get(def args) {
        String[] usage = [
            "tget -  get terminal capabilities",
            "Usage: tget [CAPABILITY]",
            "  -? --help                     Displays command help"
        ]
        Options opt = Options.compile(usage).parse(args)
        if (opt.isSet("help")) {
            throw new Options.HelpException(opt.usage())
        }
        def out = new TreeMap<>();
        def capabilities = InfoCmp.getCapabilitiesByName()
        if (!opt.args()) {
            for (Map.Entry entry : capabilities.entrySet()) {
                out[entry.key] = getCapability(entry.value) 
            }
        } else {
            String capability = opt.args().get(0)
            if (!capabilities.containsKey(capability)) {
                throw new IllegalArgumentException("Unknown capability!")
            }
            out[capability] = getCapability(capabilities.get(capability))
        }
        out;
    }
    
    def getCapability(def capability) {
        def out = null
        out = terminal.getStringCapability(capability)
        if (!out) {
            out = terminal.getNumericCapability(capability)
            if (!out) {
                out = terminal.getBooleanCapability(capability)
            }            
        }
        out
    }
    
}

def static main(def _args){
    def terminal = org.jline.console.SystemRegistry.get().consoleEngine().reader.getTerminal()
    def capability = new TerminalCapability(terminal)
    capability.get(_args)
}
