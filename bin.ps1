$op = $args[0]
$n = $args.Length

if ($n -gt 1) {
    $argv = $args[1..($n - 1)]
} else {
    $argv = ''
}

$jarFile = '.\target\jane-1.0-SNAPSHOT-jar-with-dependencies.jar'
$mainClass = 'net.pandolia.jane.MainKt'
$argvString = "-Dexec.args=`"$argv`""

switch ($op) {
    'resolve' {
        mvn dependency:resolve
        return
    }

    'clean' {
        mvn clean
        return
    }

    'compile' {
        mvn compile
        return
    }

    'start' {
        mvn exec:java $argvString
        return
    }

    'test' {
        mvn test '-Dmaven.test.skip=false'
        return
    }

    'package' {
        mvn package
        return
    }

    'jar' {
        java $argvString -cp $jarFile $mainClass
        return
    }

    'reload' {
        reload -b -d .\blog-build -p 80 -f
        return
    }

    default {
        Write-Host "Bad command: $op"
    }
}