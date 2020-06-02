// END_HELP
import org.jline.console.SystemRegistry
import org.jline.builtins.Options;

class HelloWorld {
    static void hello(def args) {
        String[] usage = [
                "hello -  test groovy script",
                "Usage: hello [NAME]",
                "  -? --help                     Displays command help",
                "     --hi                       Says hello",
        ]
        Options opt = Options.compile(usage).parse(args)
        if (opt.isSet("help")) {
            throw new Options.HelpException(opt.usage())
        }
        if (opt.isSet('hi')) {
            def who = opt.args().getAt(0) == null ? 'world' : opt.args().getAt(0)
            println "hello $who!"
        }
        def map = [user:'pippo',age:10]
        SystemRegistry.get().invoke('prnt', '-s', 'JSON', map)
    }       
}

def static main(def _args){
    HelloWorld.hello(_args)
    return 'just testing...'
}
