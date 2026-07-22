package luvoygroup.voyage.seed;

import luvoygroup.voyage.domain.Collection;
import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.DestinationSection;
import luvoygroup.voyage.domain.enums.SectionType;
import luvoygroup.voyage.repository.CollectionRepository;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.DestinationSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DestinationRepository destinationRepository;
    private final CollectionRepository collectionRepository;
    private final DestinationSectionRepository destinationSectionRepository;

    private static final List<SectionType> SECTION_ORDER = List.of(
        SectionType.ROTEIRO,
        SectionType.RESTAURANTES,
        SectionType.CAFES,
        SectionType.ATRACOES,
        SectionType.EXPERIENCIAS,
        SectionType.RUAS,
        SectionType.INSTAGRAM,
        SectionType.LOJAS,
        SectionType.ESCONDIDOS,
        SectionType.ROOFTOPS,
        SectionType.SKINCARE,
        SectionType.COMPRAS,
        SectionType.PRATOS,
        SectionType.MAPA
    );

    @Override
    @Transactional
    public void run(String... args) {
        if (destinationRepository.count() > 0) {
            log.info("Seed already applied — skipping.");
            return;
        }
        log.info("Seeding database...");
        Map<String, Destination> saved = seedDestinations();
        seedCollections(saved);
        log.info("Seed complete — {} destinations, {} collections, {} sections.",
            destinationRepository.count(),
            collectionRepository.count(),
            destinationSectionRepository.count());
    }

    // ── Destinations ─────────────────────────────────────────────────────────

    private record DestData(
        String slug, String name, String country, String city,
        String tagline, List<String> tags
    ) {}

    private static final List<DestData> DESTINATIONS = List.of(
        // Europa — 21 destinos
        new DestData("paris", "Paris", "França", "Paris",
            "Onde cada esquina guarda uma história que ainda não foi contada.",
            List.of("cultura", "gastronomia", "moda", "arte")),
        new DestData("bordeaux", "Bordeaux", "França", "Bordeaux",
            "A cidade que aprendeu a deixar o vinho ditar o ritmo.",
            List.of("vinho", "gastronomia", "arquitetura", "cultura")),
        new DestData("nice", "Nice", "França", "Nice",
            "Luz mediterrânea que pinta tudo de uma cor impossível de nomear.",
            List.of("praia", "gastronomia", "arte", "clima")),
        new DestData("marselha", "Marselha", "França", "Marselha",
            "Bruta, honesta e inesquecível como o cheiro do mar ao amanhecer.",
            List.of("cultura", "gastronomia", "história", "mar")),
        new DestData("roma", "Roma", "Itália", "Roma",
            "Dois mil anos de camadas que nenhum guia consegue esgotar.",
            List.of("história", "cultura", "gastronomia", "arte")),
        new DestData("florenca", "Florença", "Itália", "Florença",
            "O Renascimento ainda acontece aqui, em cada detalhe que você quase não vê.",
            List.of("arte", "história", "gastronomia", "arquitetura")),
        new DestData("milao", "Milão", "Itália", "Milão",
            "A cidade que trabalha enquanto o resto do mundo sonha com ela.",
            List.of("moda", "design", "gastronomia", "negócios")),
        new DestData("veneza", "Veneza", "Itália", "Veneza",
            "Uma cidade que escolheu afogar-se em beleza antes de render-se ao óbvio.",
            List.of("romance", "arquitetura", "história", "arte")),
        new DestData("costa-amalfitana", "Costa Amalfitana", "Itália", "Amalfi",
            "Onde o limão cresce entre pedra e mar, e nada mais parece necessário.",
            List.of("praia", "gastronomia", "paisagem", "romance")),
        new DestData("londres", "Londres", "Reino Unido", "Londres",
            "A cidade que inventou reinventar-se e nunca parou.",
            List.of("cultura", "história", "gastronomia", "arte")),
        new DestData("edimburgo", "Edimburgo", "Escócia", "Edimburgo",
            "Granito, névoa e uma cidade medieval que respira literatura.",
            List.of("história", "cultura", "paisagem", "arquitetura")),
        new DestData("amsterdam", "Amsterdam", "Holanda", "Amsterdam",
            "Canais que conectam bairros tão distintos quanto países.",
            List.of("cultura", "arte", "arquitetura", "ciclismo")),
        new DestData("bruxelas", "Bruxelas", "Bélgica", "Bruxelas",
            "Capital discreta com uma cena gastronômica que merece mais crédito.",
            List.of("gastronomia", "cultura", "arte", "arquitetura")),
        new DestData("bruges", "Bruges", "Bélgica", "Bruges",
            "O tempo desacelerou aqui e ninguém tentou consertar.",
            List.of("história", "romance", "arquitetura", "cultura")),
        new DestData("madrid", "Madrid", "Espanha", "Madrid",
            "A capital que só acorda de tarde e dança até o amanhecer.",
            List.of("cultura", "gastronomia", "arte", "vida noturna")),
        new DestData("barcelona", "Barcelona", "Espanha", "Barcelona",
            "Gaudí ensinou a cidade a dobrar retas e ela nunca mais parou.",
            List.of("arquitetura", "cultura", "gastronomia", "praia")),
        new DestData("sevilha", "Sevilha", "Espanha", "Sevilha",
            "O flamenco e a laranjeira amarga que perfuma cada rua.",
            List.of("cultura", "história", "gastronomia", "arte")),
        new DestData("bath", "Bath", "Reino Unido", "Bath",
            "Pedra cor de mel e termas romanas que ainda aquecem o corpo.",
            List.of("história", "arquitetura", "cultura", "termas")),
        new DestData("rotterdam", "Rotterdam", "Holanda", "Rotterdam",
            "A cidade que a guerra destruiu e os arquitetos fizeram em obra-prima.",
            List.of("arquitetura", "design", "cultura", "gastronomia")),
        new DestData("utrecht", "Utrecht", "Holanda", "Utrecht",
            "Mais tranquila que Amsterdam e igualmente encantadora, sem admitir.",
            List.of("cultura", "canais", "história", "ciclismo")),
        new DestData("ghent", "Ghent", "Bélgica", "Ghent",
            "Estudantes, medievais e cervejeiros dividem a mesma calçada de pedra.",
            List.of("história", "cultura", "gastronomia", "arte")),

        // Expansão global — 17 destinos
        new DestData("rio-de-janeiro", "Rio de Janeiro", "Brasil", "Rio de Janeiro",
            "Montanha, mar e uma energia que nenhuma outra cidade conseguiu copiar.",
            List.of("praia", "cultura", "natureza", "gastronomia")),
        new DestData("sao-paulo", "São Paulo", "Brasil", "São Paulo",
            "A maior cidade do hemisfério sul que nunca para de criar.",
            List.of("gastronomia", "arte", "cultura", "negócios")),
        new DestData("salvador", "Salvador", "Brasil", "Salvador",
            "Barroco e axé numa cidade que carrega o tempo em cada parede pintada.",
            List.of("cultura", "história", "gastronomia", "praia")),
        new DestData("florianopolis", "Florianópolis", "Brasil", "Florianópolis",
            "Quarenta e duas praias e a sensação de que sempre há uma nova a descobrir.",
            List.of("praia", "natureza", "surf", "gastronomia")),
        new DestData("buenos-aires", "Buenos Aires", "Argentina", "Buenos Aires",
            "Paris do sul — sem pedir permissão e sem precisar de comparação.",
            List.of("cultura", "gastronomia", "tango", "arquitetura")),
        new DestData("cartagena", "Cartagena", "Colômbia", "Cartagena",
            "Muros coloniais cor-de-rosa e o Caribe a dois quarteirões.",
            List.of("história", "praia", "gastronomia", "romance")),
        new DestData("nova-york", "Nova York", "EUA", "Nova York",
            "A única cidade que justifica todos os clichês escritos sobre ela.",
            List.of("cultura", "gastronomia", "arte", "moda")),
        new DestData("miami", "Miami", "EUA", "Miami",
            "Art Deco, sol e uma mistura cultural que o resto da Flórida não entende.",
            List.of("praia", "arte", "gastronomia", "vida noturna")),
        new DestData("marrakech", "Marrakech", "Marrocos", "Marrakech",
            "Os sentidos chegam antes dos olhos num labirinto que desafia qualquer mapa.",
            List.of("cultura", "história", "gastronomia", "artesanato")),
        new DestData("cidade-do-cabo", "Cidade do Cabo", "África do Sul", "Cidade do Cabo",
            "Table Mountain de um lado, o encontro de dois oceanos do outro.",
            List.of("natureza", "paisagem", "gastronomia", "aventura")),
        new DestData("zanzibar", "Zanzibar", "Tanzânia", "Zanzibar",
            "Recifes de coral, Stone Town e o perfume do cravo no ar salgado.",
            List.of("praia", "história", "snorkeling", "cultura")),
        new DestData("tokyo", "Tóquio", "Japão", "Tóquio",
            "Hipermoderna e profundamente antiga — as duas coisas ao mesmo tempo, sempre.",
            List.of("cultura", "gastronomia", "tecnologia", "arte")),
        new DestData("bali", "Bali", "Indonésia", "Ubud / Seminyak",
            "Arroz em terraços, incenso e um silêncio que a ilha conhece bem.",
            List.of("espiritualidade", "natureza", "romance", "gastronomia")),
        new DestData("bangkok", "Bangkok", "Tailândia", "Bangkok",
            "O caos que se revela uma lógica própria assim que você para de resistir.",
            List.of("cultura", "gastronomia", "templos", "vida noturna")),
        new DestData("dubai", "Dubai", "Emirados Árabes", "Dubai",
            "Deserto que decidiu não conhecer limites — e levou isso a sério demais.",
            List.of("luxo", "arquitetura", "compras", "gastronomia")),
        new DestData("singapura", "Singapura", "Singapura", "Singapura",
            "A cidade-estado que transformou eficiência em forma de arte.",
            List.of("gastronomia", "design", "cultura", "compras")),
        new DestData("kyoto", "Quioto", "Japão", "Quioto",
            "Mil templos e a certeza de que cada um merecia uma vida inteira.",
            List.of("história", "cultura", "templos", "gastronomia"))
    );

    private Map<String, Destination> seedDestinations() {
        List<Destination> entities = DESTINATIONS.stream().map(d ->
            Destination.builder()
                .slug(d.slug())
                .name(d.name())
                .country(d.country())
                .city(d.city())
                .tagline(d.tagline())
                .heroImageUrl("")
                .tags(new ArrayList<>(d.tags()))
                .active(true)
                .build()
        ).toList();

        List<Destination> savedList = destinationRepository.saveAll(entities);

        Map<String, Destination> bySlug = savedList.stream()
            .collect(Collectors.toMap(Destination::getSlug, Function.identity()));

        seedSections(savedList);
        return bySlug;
    }

    // ── Sections ──────────────────────────────────────────────────────────────

    private void seedSections(List<Destination> destinations) {
        List<DestinationSection> sections = new ArrayList<>();
        for (Destination dest : destinations) {
            for (int i = 0; i < SECTION_ORDER.size(); i++) {
                sections.add(DestinationSection.builder()
                    .destination(dest)
                    .sectionType(SECTION_ORDER.get(i))
                    .orderIndex(i)
                    .content(new ArrayList<>())
                    .build());
            }
        }
        destinationSectionRepository.saveAll(sections);
    }

    // ── Collections ───────────────────────────────────────────────────────────

    private record CollData(
        String slug, String title, String eyebrow, String italic,
        String description, int order, String[] destSlugs
    ) {}

    private static final List<CollData> COLLECTIONS = List.of(
        new CollData("romantic", "Destinos românticos", "Para dois", "para dois",
            "Os lugares que pedem silêncio, uma mesa para dois e a certeza de que há tempo.",
            0, new String[]{"paris", "veneza", "rio-de-janeiro", "bali", "bruges", "florenca"}),
        new CollData("summer", "Verão & sol", "Estação alta", "estação alta",
            "Calor, sal e aquela luz específica que só existe entre junho e agosto.",
            1, new String[]{"costa-amalfitana", "zanzibar", "miami", "cartagena", "nice", "barcelona"}),
        new CollData("fashion", "Alta moda", "Style guide", "style guide",
            "Cidades que definiram o que o mundo vai usar nos próximos dois anos.",
            2, new String[]{"paris", "milao", "nova-york", "tokyo", "londres", "dubai"}),
        new CollData("shopping", "Shopping de luxo", "Maison & atelier", "maison & atelier",
            "Do ateliê independente ao flagship — endereços que valem uma viagem.",
            3, new String[]{"paris", "milao", "singapura", "dubai", "florenca", "nova-york"}),
        new CollData("hidden-gems", "Lugares escondidos", "Off the radar", "off the radar",
            "Destinos que os guias mais populares ainda não descobriram. Por hora.",
            4, new String[]{"bordeaux", "ghent", "kyoto", "salvador", "bath", "utrecht"}),
        new CollData("coastal", "Escapadas costeiras", "Mar & sal", "mar & sal",
            "A única agenda é o horário da maré e a qualidade da luz ao entardecer.",
            5, new String[]{"costa-amalfitana", "zanzibar", "miami", "cartagena", "cidade-do-cabo"}),
        new CollData("gastronomy", "Alta gastronomia", "À mesa", "à mesa",
            "Cidades onde uma refeição pode ser o motivo inteiro da viagem.",
            6, new String[]{"paris", "sao-paulo", "singapura", "roma", "barcelona", "tokyo"}),
        new CollData("family", "Viagens em família", "Para todos", "para todos",
            "Destinos que equilibram a curiosidade dos adultos e a energia das crianças.",
            7, new String[]{"amsterdam", "londres", "roma", "barcelona", "dubai", "nova-york"}),
        new CollData("villages", "Vilarejos & cidades pequenas", "Devagar", "devagar",
            "Onde o tempo não é um recurso escasso — é o principal atrativo.",
            8, new String[]{"bruges", "ghent", "bath", "kyoto", "florianopolis", "utrecht"})
    );

    private void seedCollections(Map<String, Destination> bySlug) {
        List<Collection> collections = new ArrayList<>();
        for (CollData c : COLLECTIONS) {
            List<Destination> dests = Arrays.stream(c.destSlugs())
                .map(bySlug::get)
                .filter(d -> d != null)
                .collect(Collectors.toList());

            collections.add(Collection.builder()
                .slug(c.slug())
                .title(c.title())
                .eyebrow(c.eyebrow())
                .italic(c.italic())
                .description(c.description())
                .orderIndex(c.order())
                .destinations(dests)
                .build());
        }
        collectionRepository.saveAll(collections);
    }
}
