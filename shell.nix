{ pkgs ? import (builtins.fetchTarball {
  # Descriptive name to make the store path easier to identify
  name = "nixpkgs-unstable-2021-05-15";
  # Commit hash for nixpkgs-unstable as of 2021-05-15 from https://status.nixos.org/
  url = "https://github.com/nixos/nixpkgs/archive/a2c3ea5bf825.tar.gz";
  # Hash obtained using `nix-prefetch-url --unpack <url>`
  sha256 = "0rxn9wg73gvgb7zwzrdhranlj3jpkkcnsqmrzw5m0znwv6apj6k4";
}) {}}:

# { pkgs ? import (builtins.fetchTarball {
#   # Descriptive name to make the store path easier to identify
#   name = "nixpkgs-20.09-darwin";
#   # Commit hash for nixpkgs-20.09-darwin as of 2021-05-15 from https://status.nixos.org/
#   url = "https://github.com/nixos/nixpkgs/archive/339f21f3d46e.tar.gz";
#   # Hash obtained using `nix-prefetch-url --unpack <url>`
#   sha256 = "062h33j8ig5i22xhkcwfw79lfq90xg4vpx6xn2fib77nw793562r";
# }) {}}:

pkgs.mkShell {                  # mkShell is a helper function
  name="dev-environment";       # that requires a name
  buildInputs = with pkgs; [    # for a list of packages (search https://search.nixos.org/packages)
    # openjdk8_headless
    adoptopenjdk-hotspot-bin-8
    git
    gnupg1
    starship
  ];
  shellHook = ''             # bash to run when you enter the shell
    echo "Start developing..."
    source <(starship init bash --print-full-init)
  '';
}
