import org.jline.builtins.SystemRegistry

class HelloWorld {
    static void hello(def who) {
        println "hello $who!"
        def map = [user:'pippo',age:10]
        SystemRegistry.get().invoke('prnt', '-s', 'JSON', map)
    }       
}

def static main(def _args){
    HelloWorld.hello(!_args ? 'world' : _args[0])
    return 'just testing...'
}
