function populateVersionTag(root) {
    if (!root) return;

    fetch('version.txt')
        .then((response) => {
            if (!response.ok) {
                throw new Error(`Falha ao obter versão: ${response.status}`);
            }
            return response.text();
        })
        .then((version) => {
            root.textContent = version.trim() || '—';
        })
        .catch((error) => {
            console.error(error);
            root.textContent = 'indisponível';
        });
}

document.addEventListener('DOMContentLoaded', () => {
    const versionTargets = document.querySelectorAll('[data-version-target]');
    versionTargets.forEach(populateVersionTag);
});
