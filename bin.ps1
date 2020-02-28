switch ($args[0]) {
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

    'package' {
        mvn package
        Move-Item '.\target\jane-1.0.0-jar-with-dependencies.jar' '.\bin\jane.1.0.0.jar' -Force
        return
    }

    default {
        Write-Host "Bad command: $op"
    }
}