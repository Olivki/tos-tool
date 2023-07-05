package net.ormr.tos.cli

import com.github.ajalt.clikt.core.subcommands
import net.ormr.tos.cli.ies.IesCommand
import net.ormr.tos.cli.ies.IesPackCommand
import net.ormr.tos.cli.ies.IesTestCommand
import net.ormr.tos.cli.ies.IesUnpackCommand
import net.ormr.tos.cli.ipf.IpfCommand
import net.ormr.tos.cli.ipf.IpfPackCommand
import net.ormr.tos.cli.ipf.IpfUnpackCommand

fun main(args: Array<String>) = TosCommand()
    .subcommands(
        IesCommand().subcommands(IesUnpackCommand(), IesPackCommand(), IesTestCommand()),
        IpfCommand().subcommands(IpfUnpackCommand(), IpfPackCommand()),
    )
    .main(args)