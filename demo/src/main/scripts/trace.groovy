// END_HELP
import java.util.logging.*
import org.jline.utils.*
import org.jline.builtins.Options

class Trace {
        
    static def set(def args) {
        String[] usage = [
            "trace -  set JLine REPL console trace level",
            "Usage: trace [LEVEL]",
            "  -? --help                     Displays command help"
        ]
        Options opt = Options.compile(usage).parse(args)
        if (opt.isSet("help")) {
            throw new Options.HelpException(opt.usage())
        }
        def logger = LogManager.getLogManager().getLogger("")
        def console = org.jline.console.SystemRegistry.get().consoleEngine()
        def out
        def CONSOLE_OPTIONS = console.getVariable('CONSOLE_OPTIONS');
        def handlers = logger.getHandlers()
        if (!opt.args() || !opt.args().get(0).isInteger()) {
            out = [:]
            out['CONSOLE_OPTIONS.trace'] = CONSOLE_OPTIONS.trace
            int i = 0
            for (def h : handlers) {
                out['Log.handler' + i++] = h
            }
        } else {
            def level = opt.args().get(0).toInteger()
            if (!handlers) {
                def handler = new ConsoleHandler()
                System.setProperty("java.util.logging.SimpleFormatter.format",'%5$s%n')
                SimpleFormatter formatter = new SimpleFormatter()
                handler.setFormatter(formatter)
                logger.addHandler(handler)
                handlers = logger.getHandlers()
            }
            def tl = Level.OFF
            CONSOLE_OPTIONS.trace = level
            if (level == 2) {
                tl = Level.FINE
            } else if (level > 2) {
                tl = Level.ALL
            }
            console.putVariable('CONSOLE_OPTIONS', CONSOLE_OPTIONS)
            logger.setLevel(tl)
            handlers.each { it.setLevel(tl) }
        }
        out
    }
}

def static main(def _args){
    Trace.set(_args)
}
